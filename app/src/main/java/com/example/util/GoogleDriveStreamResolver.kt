package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ResolvedStream(
    val streamUrl: String,
    val cookieHeader: String? = null,
    val isGoogleDrive: Boolean = false,
    val fileId: String? = null,
    val previewUrl: String? = null
)

/**
 * Resolver for Google Drive video streams.
 * Resolves direct media streaming URLs and authentication cookies from public Google Drive links
 * so videos can play instantly in ExoPlayer without opening external apps or Google Play.
 */
object GoogleDriveStreamResolver {
    private const val TAG = "DriveStreamResolver"

    // Thread-safe in-memory cookie storage
    private class SimpleMemoryCookieJar : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val existing = cookieStore.getOrPut(host) { mutableListOf() }
            synchronized(existing) {
                for (cookie in cookies) {
                    existing.removeAll { it.name == cookie.name }
                    existing.add(cookie)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = mutableListOf<Cookie>()
            for ((storedHost, cookies) in cookieStore) {
                if (url.host == storedHost || url.host.endsWith(".$storedHost") || storedHost.endsWith(".${url.host}")) {
                    synchronized(cookies) {
                        list.addAll(cookies)
                    }
                }
            }
            return list
        }

        fun getCookieHeaderFor(url: HttpUrl): String {
            val cookies = loadForRequest(url)
            return cookies.joinToString("; ") { "${it.name}=${it.value}" }
        }

        fun getAllCookiesHeader(): String {
            val all = mutableListOf<String>()
            for (cookies in cookieStore.values) {
                synchronized(cookies) {
                    for (c in cookies) {
                        all.add("${c.name}=${c.value}")
                    }
                }
            }
            return all.distinct().joinToString("; ")
        }
    }

    private val cookieJar = SimpleMemoryCookieJar()

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a given URL is a Google Drive URL.
     */
    fun isGoogleDriveUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("drive.google.com") ||
                lower.contains("docs.google.com") ||
                lower.contains("drive.usercontent.google.com")
    }

    /**
     * Extracts the Google Drive file ID from various link formats.
     */
    fun extractGoogleDriveFileId(url: String): String? {
        val clean = url.trim()
        val regexes = listOf(
            Regex("""/file/d/([a-zA-Z0-9_-]{20,})"""),
            Regex("""(?:[?&])id=([a-zA-Z0-9_-]{20,})"""),
            Regex("""(?:open|uc)\?id=([a-zA-Z0-9_-]{20,})"""),
            Regex("""/d/([a-zA-Z0-9_-]{20,})""")
        )
        for (regex in regexes) {
            val match = regex.find(clean)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Attempts to resolve the direct media streaming link and session cookies for a Google Drive file.
     * Follows redirects to drive.usercontent.google.com, parses virus scan confirmations,
     * and extracts the direct video stream URL for ExoPlayer.
     */
    suspend fun resolveStream(rawUrl: String): ResolvedStream = withContext(Dispatchers.IO) {
        val clean = rawUrl.trim()
        if (!isGoogleDriveUrl(clean)) {
            return@withContext ResolvedStream(
                streamUrl = clean,
                cookieHeader = null,
                isGoogleDrive = false,
                fileId = null
            )
        }

        val fileId = extractGoogleDriveFileId(clean)
        if (fileId == null) {
            Log.w(TAG, "Could not extract Google Drive file ID from $clean")
            return@withContext ResolvedStream(
                streamUrl = clean,
                cookieHeader = null,
                isGoogleDrive = true,
                fileId = null,
                previewUrl = clean
            )
        }

        val previewUrl = "https://drive.google.com/file/d/$fileId/preview"
        val candidateDownloadUrl = "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
        val googleUcUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"

        try {
            // First step: Attempt to query the direct download endpoint
            val request = Request.Builder()
                .url(candidateDownloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type") ?: ""
                val cookies = cookieJar.getAllCookiesHeader()

                // Check if response is directly serving media
                if (contentType.startsWith("video/") ||
                    contentType.startsWith("application/octet-stream") ||
                    (response.isSuccessful && !contentType.contains("text/html"))
                ) {
                    Log.d(TAG, "Direct video media stream found: $finalUrl ($contentType)")
                    return@withContext ResolvedStream(
                        streamUrl = finalUrl,
                        cookieHeader = cookies.ifBlank { null },
                        isGoogleDrive = true,
                        fileId = fileId,
                        previewUrl = previewUrl
                    )
                }

                // If HTML is returned, it may be the Google Drive virus scan warning page with confirm tokens
                val htmlBody = response.body?.string() ?: ""

                // 1. Look for form action in the warning page
                val formActionRegex = Regex("""<form[^>]*action=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
                val formActionMatch = formActionRegex.find(htmlBody)

                if (formActionMatch != null) {
                    var actionUrl = formActionMatch.groupValues[1].replace("&amp;", "&")
                    if (actionUrl.startsWith("/")) {
                        actionUrl = "https://drive.usercontent.google.com$actionUrl"
                    }

                    // Extract all hidden inputs (id, export, confirm, uuid, etc.)
                    val inputRegex = Regex("""<input[^>]+name=["']([^"']+)["'][^>]+value=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                    val inputMatches = inputRegex.findAll(htmlBody)
                    val queryParams = mutableListOf<String>()

                    for (m in inputMatches) {
                        val name = m.groupValues[1]
                        val value = m.groupValues[2]
                        queryParams.add("${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}")
                    }

                    if (queryParams.isNotEmpty()) {
                        val separator = if (actionUrl.contains("?")) "&" else "?"
                        val fullStreamUrl = "$actionUrl$separator${queryParams.joinToString("&")}"
                        Log.d(TAG, "Form confirm stream URL resolved: $fullStreamUrl")
                        return@withContext ResolvedStream(
                            streamUrl = fullStreamUrl,
                            cookieHeader = cookies.ifBlank { null },
                            isGoogleDrive = true,
                            fileId = fileId,
                            previewUrl = previewUrl
                        )
                    }
                }

                // 2. Look for download link directly with confirm param
                val linkRegex = Regex("""href=["'](https://drive\.usercontent\.google\.com/download\?[^"']+)["']""")
                val linkMatch = linkRegex.find(htmlBody)
                if (linkMatch != null) {
                    val streamUrl = linkMatch.groupValues[1].replace("&amp;", "&")
                    Log.d(TAG, "Download link found in HTML: $streamUrl")
                    return@withContext ResolvedStream(
                        streamUrl = streamUrl,
                        cookieHeader = cookies.ifBlank { null },
                        isGoogleDrive = true,
                        fileId = fileId,
                        previewUrl = previewUrl
                    )
                }

                // 3. Look for relative /uc? link
                val ucRegex = Regex("""href=["'](/uc\?[^"']+)["']""")
                val ucMatch = ucRegex.find(htmlBody)
                if (ucMatch != null) {
                    val streamUrl = "https://drive.google.com" + ucMatch.groupValues[1].replace("&amp;", "&")
                    Log.d(TAG, "Relative uc link found: $streamUrl")
                    return@withContext ResolvedStream(
                        streamUrl = streamUrl,
                        cookieHeader = cookies.ifBlank { null },
                        isGoogleDrive = true,
                        fileId = fileId,
                        previewUrl = previewUrl
                    )
                }
            }

            // Step 2: Fallback check on standard Google UC link
            val request2 = Request.Builder()
                .url(googleUcUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .get()
                .build()

            client.newCall(request2).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val cookies = cookieJar.getAllCookiesHeader()
                if (finalUrl.contains("drive.usercontent.google.com")) {
                    Log.d(TAG, "UC redirect resolved: $finalUrl")
                    return@withContext ResolvedStream(
                        streamUrl = finalUrl,
                        cookieHeader = cookies.ifBlank { null },
                        isGoogleDrive = true,
                        fileId = fileId,
                        previewUrl = previewUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception resolving Google Drive stream: ${e.message}", e)
        }

        // Default candidate URL
        val cookies = cookieJar.getAllCookiesHeader()
        return@withContext ResolvedStream(
            streamUrl = candidateDownloadUrl,
            cookieHeader = cookies.ifBlank { null },
            isGoogleDrive = true,
            fileId = fileId,
            previewUrl = previewUrl
        )
    }

    /**
     * Backward-compatible helper method.
     */
    suspend fun resolveDirectStreamUrl(rawUrl: String): String {
        return resolveStream(rawUrl).streamUrl
    }
}
