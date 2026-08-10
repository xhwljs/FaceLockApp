package com.insightface.recognizer.update

import com.insightface.recognizer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub Releases for a newer version than the installed [BuildConfig.VERSION_NAME].
 *
 * Endpoint: `GET https://api.github.com/repos/{owner}/{repo}/releases/latest`
 * Docs: https://docs.github.com/rest/releases/releases#get-the-latest-release
 *
 * The owner/repo come from BuildConfig fields injected at build time (see app/build.gradle.kts
 * GITHUB_REPO_OWNER / GITHUB_REPO_NAME). Override them in your fork's gradle.properties.
 */
class UpdateChecker {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class CheckResult(
        val hasUpdate: Boolean,
        val currentVersion: String,
        val latestVersion: String,
        val release: GitHubRelease?,
        val forceUpdate: Boolean = false,
        val error: String? = null,
    )

    suspend fun checkLatest(): CheckResult = withContext(Dispatchers.IO) {
        val owner = BuildConfig.GITHUB_REPO_OWNER
        val repo = BuildConfig.GITHUB_REPO_NAME
        val endpoint = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val current = BuildConfig.VERSION_NAME
        try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "InsightFaceRecognizer-AppUpdate")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            // The /releases/latest endpoint never returns a prerelease, but double-check so
            // app users never get offered a test build even if the API contract changes.
            if (conn.responseCode != 200) {
                val msg = conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(200)
                return@withContext CheckResult(
                    hasUpdate = false,
                    currentVersion = current,
                    latestVersion = current,
                    release = null,
                    error = "GitHub API ${conn.responseCode}: ${msg ?: "请求失败"} ($endpoint)",
                )
            }
            conn.inputStream.use { stream ->
                val body = stream.bufferedReader().use { it.readText() }
                val release = json.decodeFromString(GitHubRelease.serializer(), body)
                if (release.preRelease) {
                    return@withContext CheckResult(
                        hasUpdate = false,
                        currentVersion = current,
                        latestVersion = current,
                        release = null,
                        error = "最新版本为预发布版，已跳过",
                    )
                }
                val latest = release.tagName.removePrefix("v").removePrefix("V")
                CheckResult(
                    hasUpdate = isNewer(latest, current),
                    currentVersion = current,
                    latestVersion = latest,
                    release = release,
                    forceUpdate = release.isForceUpdate,
                )
            }
        } catch (e: Exception) {
            CheckResult(false, current, current, null, error = "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Returns true if [latest] is a higher semantic version than [current]. */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = parseSemver(latest)
        val c = parseSemver(current)
        if (l == null || c == null) return latest != current && latest.isNotEmpty()
        if (l[0] != c[0]) return l[0] > c[0]
        if (l[1] != c[1]) return l[1] > c[1]
        return l[2] > c[2]
    }

    private fun parseSemver(v: String): List<Int>? {
        val core = v.substringBefore("-").substringBefore("+")
        val parts = core.split(".")
        if (parts.isEmpty()) return null
        return parts.mapNotNull { it.toIntOrNull() }.takeIf { it.size >= 3 }
            ?: (parts.mapNotNull { it.toIntOrNull() } + listOf(0, 0)).take(3).takeIf { it.isNotEmpty() }
    }
}
