package com.parentalguard.parent.update

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Checks GitHub releases for a newer child APK.
 *
 * Flow: parent (has UI + internet) queries the latest release, downloads the
 * child APK asset once, then pushes it to each paired child over LAN
 * (POST /update). The child installs silently when it is Device Owner,
 * otherwise Android shows the normal install-confirmation prompt.
 */
object GitHubReleaseChecker {
    const val RELEASES_URL = "https://api.github.com/repos/H-Ossama/Family-Guard/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class GHAsset(
        val name: String = "",
        val browser_download_url: String = "",
        val size: Long = 0L
    )

    @Serializable
    private data class GHRelease(
        val tag_name: String = "",
        val name: String = "",
        val body: String = "",
        val assets: List<GHAsset> = emptyList()
    )

    data class ChildReleaseInfo(
        val versionTag: String, // e.g. "v2.4.8"
        val title: String,
        val notes: String,
        val downloadUrl: String,
        val assetName: String,
        val assetSize: Long
    )

    suspend fun fetchLatestChildRelease(): ChildReleaseInfo? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
        }
        try {
            val release: GHRelease = client.get(RELEASES_URL) {
                header("Accept", "application/vnd.github+json")
            }.body()
            if (release.tag_name.isBlank()) return null
            // Prefer an APK whose name mentions "child"; fall back to any APK.
            val asset = release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true) &&
                    it.name.contains("child", ignoreCase = true)
            } ?: release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true)
            } ?: return null
            return ChildReleaseInfo(
                versionTag = release.tag_name,
                title = release.name.ifBlank { release.tag_name },
                notes = release.body,
                downloadUrl = asset.browser_download_url,
                assetName = asset.name,
                assetSize = asset.size
            )
        } catch (_: Exception) {
            return null
        } finally {
            client.close()
        }
    }

    /** True when [latestTag] (e.g. "v2.4.8") is newer than [current] (e.g. "2.4.7"). */
    fun isNewerVersion(latestTag: String, current: String?): Boolean {
        if (current.isNullOrBlank()) return true
        fun parts(v: String) = v.trim().trimStart('v', 'V')
            .split('.', '-')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val l = parts(latestTag)
        val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val li = l.getOrElse(i) { 0 }
            val ci = c.getOrElse(i) { 0 }
            if (li != ci) return li > ci
        }
        return false
    }

    fun cleanTag(tag: String): String = tag.trim().trimStart('v', 'V')
}
