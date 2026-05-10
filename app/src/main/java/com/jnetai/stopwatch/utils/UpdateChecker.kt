package com.jnetai.stopwatch.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * UpdateChecker - Checks for app updates from GitHub releases.
 * Uses the GitHub API to fetch the latest release tag and compares with
 * the current version.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/jnetai/StopWatch/releases/latest"
    private const val GITHUB_RELEASES_URL = "https://github.com/jnetai/StopWatch/releases"
    private const val CONNECT_TIMEOUT = 8000
    private const val READ_TIMEOUT = 8000

    data class UpdateInfo(
        val latestVersion: String,
        val releaseUrl: String,
        val isUpdateAvailable: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Check for updates from GitHub API.
     * Performs network request on the calling thread - should be called from a coroutine/background thread.
     */
    fun checkForUpdate(context: Context, currentVersion: String): UpdateInfo {
        if (!isNetworkAvailable(context)) {
            return UpdateInfo("", "", false,
                "No network connection available. Please check your internet.")
        }

        return ErrorLogger.tryOrNull(ErrorLogger.Codes.GEN_UPDATE_CHECK,
            "Failed to check for updates") ?.let { result ->
            result
        } ?: run {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "StopWatch-Android")

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // Parse the JSON to get the tag_name
                    val tagName = extractTagName(response)
                    if (tagName != null) {
                        val isUpdate = compareVersions(currentVersion, tagName) < 0
                        UpdateInfo(
                            latestVersion = tagName,
                            releaseUrl = GITHUB_RELEASES_URL,
                            isUpdateAvailable = isUpdate
                        )
                    } else {
                        UpdateInfo("", "", false,
                            "Could not parse update information.")
                    }
                } else {
                    UpdateInfo("", "", false,
                        "GitHub API returned status $responseCode")
                }
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.GEN_UPDATE_CHECK,
                    "Network request failed", e)
                UpdateInfo("", "", false,
                    "Network error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Get the GitHub releases URL for manual checking.
     */
    fun getReleasesUrl(): String = GITHUB_RELEASES_URL

    /**
     * Simple JSON tag_name extraction (no external JSON library dependency).
     */
    private fun extractTagName(json: String): String? {
        return try {
            val key = "\"tag_name\":"
            val startIndex = json.indexOf(key)
            if (startIndex >= 0) {
                val valueStart = startIndex + key.length
                val quoteStart = json.indexOf('"', valueStart)
                if (quoteStart >= 0) {
                    val quoteEnd = json.indexOf('"', quoteStart + 1)
                    if (quoteEnd >= 0) {
                        json.substring(quoteStart + 1, quoteEnd)
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UPDATE_CHECK,
                "Failed to parse tag_name", e)
            null
        }
    }

    /**
     * Compare two semantic version strings.
     * Returns negative if v1 < v2, positive if v1 > v2, 0 if equal.
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val cleanV1 = v1.trimStart('v', 'V')
        val cleanV2 = v2.trimStart('v', 'V')

        val parts1 = cleanV1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = cleanV2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1 - p2
            }
        }
        return 0
    }

    /**
     * Check if network is available.
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }
}
