package net.zhuruoling.nmsl.minecraft.mod.repo

import com.google.gson.annotations.SerializedName
import net.zhuruoling.nmsl.minecraft.mod.loader.ModLoader
import net.zhuruoling.nmsl.util.gson
import net.zhuruoling.nmsl.util.userAgent
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

    override fun findMod(minecraftVersion: String, modLoader: ModLoader, modId: String, modVersion: String): Mod? {
        val route = "project/$modId/version?loaders=[%22${modLoader.id}%22]&game_versions=[%22$minecraftVersion%22]"
        val responseJson = getApi(route) ?: return null
        val response = gson.fromJson(responseJson, Array<ModrinthModItem>::class.java)
        try{
            response.first {
                if (modVersion == "latest") true
                else (modVersion == it.versionNumber)
            }.apply {
                val modFileItem = files.first { it.primary }
                return Mod(
                    minecraftVersion,
                    modLoader,
                    modId,
                    modFileItem.url,
                    modFileItem.hashes.sha1,
                    modFileItem.size,
                    modFileItem.filename
                )
            }
        }catch (e:NoSuchElementException){
            return null
        }

    }

    data class ModrinthModItem(
        @SerializedName("version_number") val versionNumber: String,
        @SerializedName("name") val name: String,
        @SerializedName("version_type") val versionType: String,
        @SerializedName("game_versions") val gameVersions: List<String>,
        @SerializedName("loaders") val loaders: List<String>,
        @SerializedName("files") val files: List<ModrinthModFileItem>
    )

    data class ModrinthModFileItem(
        val hashes: HashItem,
        val url: String,
        val filename: String,
        val primary: Boolean,
        val size: Long
    )

    data class HashItem(val sha1: String, val sha256: String)
}