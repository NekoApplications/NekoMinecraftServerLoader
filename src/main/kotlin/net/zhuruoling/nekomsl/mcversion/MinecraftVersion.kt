package net.zhuruoling.nekomsl.mcversion

import com.google.gson.annotations.SerializedName
import net.zhuruoling.nekomsl.cache.FileMetadata
import net.zhuruoling.nekomsl.util.gson
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

object MinecraftVersion {
    //https://piston-meta.mojang.com/mc/game/version_manifest.json
    private val mojangApiUrl = System.getenv("mojangApiUrl") ?: "https://piston-meta.mojang.com/"
    private val versionManifestUri = URL("$mojangApiUrl/mc/game/version_manifest.json").toURI()
    private lateinit var versionManifest: VersionManifest
    private val httpClient = HttpClient.newHttpClient()
    private lateinit var latestStableVersion: String
    private lateinit var latestVersion: String
    private val versions = mutableMapOf<String, VersionData>()
    fun update() {
        val request = HttpRequest.newBuilder().GET().uri(versionManifestUri).build()
        httpClient.send(request, BodyHandlers.ofString()).also {
            val resp = it.body()
            versionManifest = gson.fromJson(resp, VersionManifest::class.java)
            versions.clear()
            versions += versionManifest.versions.map { v -> v.id to v }
            latestStableVersion = versionManifest.latest.release
            latestVersion = versionManifest.latest.snapshot
        }
    }

    operator fun get(minecraftVersion: String): VersionData? {
        return versions[minecraftVersion]
    }

    fun parseVersion(version: String): String {
        return when (version) {
            "latest" -> latestVersion
            "latestStable" -> latestStableVersion
            else -> {
                if (version !in versions) throw IllegalArgumentException("$version is not a valid version name.")
                version
            }
        }
    }

    fun resolveVersionDataForDownload(version: String): FileMetadata {
        val exactVersion = parseVersion(version)
        val data = versions[exactVersion]!!
        val request = HttpRequest.newBuilder().GET().uri(URL(data.url).toURI()).build()
        val serverDownloadData = httpClient.send(request, BodyHandlers.ofString(Charsets.UTF_8)).run {
            val versionMetadata = gson.fromJson(this.body(), VersionMetadata::class.java)
            versionMetadata.downloads["server"]
                ?: throw IllegalArgumentException("Cannot retrieve server download url from mojang.")
        }
        return FileMetadata("$version-server.jar",serverDownloadData.url, serverDownloadData.size, serverDownloadData.sha1)
    }
}

data class VersionMetadata(val id: String, val downloads: Map<String, VersionDownloadsData>)

data class VersionDownloadsData(val sha1: String, val size: Long, val url: String)

data class LatestData(val release: String, val snapshot: String)
enum class VersionType {
    @SerializedName("snapshot")
    SNAPSHOT,

    @SerializedName("release")
    RELEASE,

    @SerializedName("old_alpha")
    OLD_ALPHA,

}

data class VersionData(
    val id: String,
    val type: VersionType,
    val url: String,
    val releaseTime: String,
    val time: String
)

data class VersionManifest(val latest: LatestData, val versions: MutableList<VersionData>)