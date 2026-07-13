package icu.takeneko.nekomsl.minecraft.mod.repo

import icu.takeneko.nekomsl.minecraft.mod.loader.ModLoader
import icu.takeneko.nekomsl.util.json
import icu.takeneko.nekomsl.util.userAgent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.NoSuchElementException

object ModrinthModRepository : ModRepository("modrinth") {
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    private fun getApi(route: String): String? {
        val uri = URL("https://api.modrinth.com/v2/$route").toURI()
        val request = HttpRequest.newBuilder(uri).setHeader("User-Agent", userAgent).GET().build()
        val result = httpClient.send(request, BodyHandlers.ofString(Charsets.UTF_8))
        return if (result.statusCode() == 200) result.body() else null
    }

    override fun findMod(
        minecraftVersion: String,
        modLoader: ModLoader,
        modId: String,
        modVersion: String
    ): Pair<Mod?, List<Mod>> {
        val route = "project/$modId/version?loaders=[%22${modLoader.id}%22]&game_versions=[%22$minecraftVersion%22]"
        val responseJson = getApi(route) ?: return null to emptyList()
        val response = json.decodeFromString<List<ModrinthModItem>>(responseJson)
        val alternative = try {
            if (response.isEmpty()) return null to emptyList()
            response.map {
                val modFileItem = it.files.first { item -> item.primary }
                Mod(
                    minecraftVersion,
                    modLoader,
                    modId,
                    modFileItem.url,
                    modFileItem.hashes.sha1,
                    modFileItem.size,
                    modFileItem.filename,
                    it.versionNumber
                )
            }
        }catch (e: NoSuchElementException) {
            e.printStackTrace()
            return null to emptyList()
        }
        return try {
            alternative.first {
                if (modVersion == "latest") true
                else (modVersion == it.version)
            } to alternative
        } catch (e: NoSuchElementException) {
            null to alternative
        }
    }

    @Serializable
    data class ModrinthModItem(
        @SerialName("version_number") val versionNumber: String,
        @SerialName("name") val name: String,
        @SerialName("version_type") val versionType: String,
        @SerialName("game_versions") val gameVersions: List<String>,
        @SerialName("loaders") val loaders: List<String>,
        @SerialName("files") val files: List<ModrinthModFileItem>
    )

    @Serializable
    data class ModrinthModFileItem(
        val hashes: HashItem,
        val url: String,
        val filename: String,
        val primary: Boolean,
        val size: Long
    )

    @Serializable
    data class HashItem(val sha1: String, val sha256: String)
}
