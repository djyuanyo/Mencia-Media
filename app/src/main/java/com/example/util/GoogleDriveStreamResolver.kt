package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Resolver for Google Drive video streams.
 * Resolves direct media streaming URLs from public Google Drive links
 * so videos can play instantly in a custom native video player.
 */
object GoogleDriveStreamResolver {
    private const val TAG = "DriveStreamResolver"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a given URL is a Google Drive URL.
     */
    fun isGoogleDriveUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("drive.google.com") || lower.contains("docs.google.com")
    }

    /**
     * Extracts the Google Drive file ID from various link formats.
     */
    fun extractGoogleDriveFileId(url: String): String? {
        val clean = url.trim()
        val regex = Regex("""(?:/file/d/|id=|open\?id=|uc\?id=)([a-zA-Z0-9_-]{20,})""")
        return regex.find(clean)?.groupValues?.get(1)
    }

    /**
     * Attempts to resolve the direct media streaming link for a Google Drive file.
     * Follows redirects to drive.usercontent.google.com and handles virus scan confirmations.
     */
    suspend fun resolveDirectStreamUrl(rawUrl: String): String = withContext(Dispatchers.IO) {
        val clean = rawUrl.trim()
        if (!isGoogleDriveUrl(clean)) {
            return@withContext clean
        }

        val fileId = extractGoogleDriveFileId(clean)
        if (fileId == null) {
            Log.w(TAG, "Could not extract Google Drive file ID from $clean")
            return@withContext clean
        }

        // Direct download stream URL with confirmation parameter
        val candidateUrl = "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
        val fallbackUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"

        try {
            val request = Request.Builder()
                .url(candidateUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .head() // Try HEAD request first for fast redirect resolution
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type") ?: ""

                if (response.isSuccessful || response.code in 200..399) {
                    if (finalUrl.contains("drive.usercontent.google.com") || contentType.startsWith("video/") || contentType.startsWith("application/octet-stream")) {
                        Log.d(TAG, "Successfully resolved direct stream URL: $finalUrl")
                        return@withContext finalUrl
                    }
                }
            }

            // Secondary check with fallback URL
            val request2 = Request.Builder()
                .url(fallbackUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .get()
                .build()

            client.newCall(request2).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (finalUrl.contains("drive.usercontent.google.com")) {
                    return@withContext finalUrl
                }

                // Check HTML response for confirmation action URL
                val body = response.body?.string() ?: ""
                val actionMatch = Regex("""action=["'](https://drive\.usercontent\.google\.com/[^"']+)["']""").find(body)
                if (actionMatch != null) {
                    val streamUrl = actionMatch.groupValues[1].replace("&amp;", "&")
                    Log.d(TAG, "Extracted direct stream URL from action: $streamUrl")
                    return@withContext streamUrl
                }

                val linkMatch = Regex("""href=["'](/uc\?[^"']+)["']""").find(body)
                if (linkMatch != null) {
                    val streamUrl = "https://drive.google.com" + linkMatch.groupValues[1].replace("&amp;", "&")
                    return@withContext streamUrl
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving Google Drive direct URL: ${e.message}")
        }

        return@withContext candidateUrl
    }
}
