package icu.takeneko.nekomsl.mcversion

import icu.takeneko.nekomsl.cache.CacheProvider
import icu.takeneko.nekomsl.cache.FileMetadata
import icu.takeneko.nekomsl.util.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object MinecraftVersion {
    //https://piston-meta.mojang.com/mc/game/version_manifest.json
    private val mojangApiUrl = System.getenv("mojangApiUrl") ?: "https://piston-meta.mojang.com/"
    private val versionManifestUri = URL("$mojangApiUrl/mc/game/version_manifest.json").toURI()
    private val versionManifestCacheFile = CacheProvider.cacheRoot.resolve("minecraft-version-manifest.json")
    private lateinit var versionManifest: VersionManifest
    private val httpClient = HttpClient.newHttpClient()
    private val logger = LoggerFactory.getLogger("MinecraftVersion")
    private lateinit var latestStableVersion: String
    private lateinit var latestVersion: String
    private val versions = mutableMapOf<String, VersionData>()

    fun update() {
        val manifestJson = fetchVersionManifestJson()
        versionManifest = json.decodeFromString<VersionManifest>(manifestJson)
        versions.clear()
        versions += versionManifest.versions.map { v -> v.id to v }
        latestStableVersion = versionManifest.latest.release
        latestVersion = versionManifest.latest.snapshot
    }

    private fun fetchVersionManifestJson(): String {
        val remoteFailure = fetchRemoteVersionManifestJson()
        if (remoteFailure == null) {
            return versionManifestCacheFile.readText()
        }
        logger.warn("Cannot update Minecraft version manifest from remote after 5 attempts, trying local cache.", remoteFailure)
        if (!versionManifestCacheFile.exists()) {
            throw IllegalStateException(
                "Cannot update Minecraft version manifest from remote and local cache does not exist.",
                remoteFailure
            )
        }
        return try {
            versionManifestCacheFile.readText()
        } catch (e: Exception) {
            throw IllegalStateException("Cannot read local Minecraft version manifest cache.", e).also {
                it.addSuppressed(remoteFailure)
            }
        }
    }

    private fun fetchRemoteVersionManifestJson(): Throwable? {
        val request = HttpRequest.newBuilder().GET().uri(versionManifestUri).build()
        var lastFailure: Throwable? = null
        repeat(5) { index ->
            try {
                logger.info("Updating Minecraft version manifest from remote, attempt ${index + 1}/5.")
                val response = httpClient.send(request, BodyHandlers.ofString(Charsets.UTF_8))
                if (response.statusCode() !in 200..299) {
                    throw IllegalStateException("HTTP ${response.statusCode()} while requesting $versionManifestUri")
                }
                val manifestJson = response.body()
                json.decodeFromString<VersionManifest>(manifestJson)
                versionManifestCacheFile.parent.createDirectories()
                versionManifestCacheFile.writeText(manifestJson)
                logger.info("Minecraft version manifest cache updated at $versionManifestCacheFile.")
                return null
            } catch (e: Exception) {
                lastFailure = e
                logger.warn("Minecraft version manifest update attempt ${index + 1}/5 failed.", e)
            }
        }
        return lastFailure ?: IllegalStateException("Minecraft version manifest update failed without an exception.")
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
            val versionMetadata = json.decodeFromString<VersionMetadata>(this.body())
            versionMetadata.downloads["server"]
                ?: throw IllegalArgumentException("Cannot retrieve server download url from mojang.")
        }
        return FileMetadata("$version-server.jar",serverDownloadData.url, serverDownloadData.size, serverDownloadData.sha1)
    }
}

@Serializable
data class VersionMetadata(val id: String, val downloads: Map<String, VersionDownloadsData>)

@Serializable
data class VersionDownloadsData(val sha1: String, val size: Long, val url: String)

@Serializable
data class LatestData(val release: String, val snapshot: String)
@Serializable
enum class VersionType {
    @SerialName("snapshot")
    SNAPSHOT,

    @SerialName("release")
    RELEASE,

    @SerialName("old_alpha")
    OLD_ALPHA,

    @SerialName("old_beta")
    OLD_BETA,

}

@Serializable
data class VersionData(
    val id: String,
    val type: VersionType,
    val url: String,
    val releaseTime: String,
    val time: String
)

@Serializable
data class VersionManifest(val latest: LatestData, val versions: MutableList<VersionData>)
