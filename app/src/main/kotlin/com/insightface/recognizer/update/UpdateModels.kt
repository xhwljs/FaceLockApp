package com.insightface.recognizer.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Subset of the GitHub "Get the latest release" response we actually use. */
@Serializable
data class GitHubRelease(
    val url: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("prerelease") val preRelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<Asset> = emptyList(),
) {
    @Serializable
    data class Asset(
        val name: String = "",
        val size: Long = 0,
        @SerialName("browser_download_url") val browserDownloadUrl: String = "",
        @SerialName("content_type") val contentType: String = "",
    ) {
        val isApk: Boolean get() = name.endsWith(".apk", ignoreCase = true)
    }

    /** The first .apk asset, or null. */
    val apkAsset: Asset? get() = assets.firstOrNull { it.isApk }
}
