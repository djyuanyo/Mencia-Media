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
    val fileId: String? = null
)

/**
 * Resolver for Google Drive video streams.
 * Resolves direct media streaming URLs and session cookies from public Google Drive links
 * so ExoPlayer can stream the raw video directly with hardware decoding,
 * completely avoiding any embedded Google Drive web players or Google Play popups.
 */
object GoogleDriveStreamResolver {
    private const val TAG = "DriveStreamResolver"
    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

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
                fileId = null
            )
        }

        // Primary download endpoints on drive.google.com that initiate authentication and redirect
        val initialUrl = "https://drive.google.com/uc?id=$fileId&export=download&confirm=t"

        try {
            val request = Request.Builder()
                .url(initialUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "*/*")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type") ?: ""
                val cookies = cookieJar.getAllCookiesHeader()

                // If redirected to drive.usercontent.google.com and it's video or binary data
                if (finalUrl.contains("drive.usercontent.google.com") &&
                    (contentType.startsWith("video/") ||
                     contentType.startsWith("application/octet-stream") ||
                     !contentType.contains("text/html"))
                ) {
                    Log.d(TAG, "Direct Google Drive video stream found via redirect: $finalUrl ($contentType)")
                    return@withContext ResolvedStream(
                        streamUrl = finalUrl,
                        cookieHeader = cookies.ifBlank { null },
                        isGoogleDrive = true,
                        fileId = fileId
                    )
                }

                // If response is HTML, it may be the Google Drive virus scan warning for files > 100MB
                val htmlBody = response.body?.string() ?: ""

                // 1. Look for form action in warning page
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

                        // Follow redirect on the confirmed action URL
                        val confirmReq = Request.Builder()
                            .url(fullStreamUrl)
                            .header("User-Agent", BROWSER_USER_AGENT)
                            .header("Accept", "*/*")
                            .head()
                            .build()

                        client.newCall(confirmReq).execute().use { confirmResp ->
                            val confirmedFinal = confirmResp.request.url.toString()
                            val updatedCookies = cookieJar.getAllCookiesHeader()
                            Log.d(TAG, "Confirmed final video stream URL: $confirmedFinal")
                            return@withContext ResolvedStream(
                                streamUrl = confirmedFinal,
                                cookieHeader = updatedCookies.ifBlank { null },
                                isGoogleDrive = true,
                                fileId = fileId
                            )
                        }
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
                        fileId = fileId
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
                        fileId = fileId
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception resolving Google Drive stream: ${e.message}", e)
        }

        // Direct candidate URL fallback on drive.usercontent.google.com
        val fallbackDirectUrl = "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
        val cookies = cookieJar.getAllCookiesHeader()
        return@withContext ResolvedStream(
            streamUrl = fallbackDirectUrl,
            cookieHeader = cookies.ifBlank { null },
            isGoogleDrive = true,
            fileId = fileId
        )
    }

    /**
     * Backward-compatible helper method.
     */
    suspend fun resolveDirectStreamUrl(rawUrl: String): String {
        return resolveStream(rawUrl).streamUrl
    }
}
