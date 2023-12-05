package net.zhuruoling.nmsl.minecraft.mod.loader

import cn.hutool.http.HttpUtil
import cn.hutool.json.XML
import net.zhuruoling.nmsl.cache.FileMetadata
import org.apache.commons.codec.digest.DigestUtils
import java.beans.XMLDecoder
import java.nio.file.Path
import java.util.regex.Pattern

object FabricModLoaderInstaller : ModLoaderInstaller("fabric") {

    val versionRegex = Pattern.compile(".+<release>([0-9.a-zA-Z]+)</release>.+")

    override fun install(installerPath: Path, path: Path, minecraftVersion: String, loaderVersion: String) {

    }

    override fun alreadyInstalledModLoader(serverRoot: Path): Boolean {
        TODO("Not yet implemented")
    }

    fun getLatestInstallerVersion():String{
        val mvnXml = HttpUtil.downloadString("https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml", Charsets.UTF_8).replace("\n","").replace("\r","")
        val matcher = versionRegex.matcher(mvnXml)
        if (!matcher.matches()){
            throw RuntimeException("Retrieve latest installer version from fabric maven failed.")
        }
        return matcher.group(1)
    }

    override fun fetchDownloadInfo(minecraftVersion: String, loaderVersion: String): FileMetadata {
        val latestInstallerVersion = getLatestInstallerVersion()
        val downloadUrl =
            "https://maven.fabricmc.net/net/fabricmc/fabric-installer/$latestInstallerVersion/fabric-installer-$latestInstallerVersion.jar"
        val sha1Url = "$downloadUrl.sha1"
        val sha1 = HttpUtil.get(sha1Url)
        return FileMetadata("fabric-installer-$latestInstallerVersion.jar", downloadUrl, 0L, sha1)
    }
}