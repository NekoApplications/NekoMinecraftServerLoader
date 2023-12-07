package net.zhuruoling.nekomsl.minecraft.mod.loader

import cn.hutool.core.io.IORuntimeException
import cn.hutool.http.HttpUtil
import net.zhuruoling.nekomsl.cache.CacheProvider
import net.zhuruoling.nekomsl.cache.FileMetadata
import net.zhuruoling.nekomsl.task.minecraft.ServerConfigureTaskContext
import net.zhuruoling.nekomsl.util.withStdoutRedirectedToSlf4j
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.*

object FabricModLoaderInstaller : ModLoaderInstaller("fabric") {

    val versionRegex = Pattern.compile(".+<release>([0-9.a-zA-Z]+)</release>.+")
    lateinit var installerDownloadInfo: FileMetadata

    override fun install(
        installerPath: Path,
        path: Path,
        minecraftVersion: String,
        loaderVersion: String,
        context: ServerConfigureTaskContext
    ): String {
        val classLoader = URLClassLoader(arrayOf(installerPath.toFile().toURI().toURL()), this::class.java.classLoader)
        val installerMainClass = classLoader.loadClass("net.fabricmc.installer.Main")
        val main = installerMainClass.getDeclaredConstructor().newInstance()
        val mainMethod = installerMainClass.getMethod("main", Array<String>::class.java)
        val args = arrayOf(
            "server",
            "-dir",
            path.toAbsolutePath().toString(),
            "-mcversion",
            minecraftVersion,
            "-loader",
            loaderVersion
        )
        withStdoutRedirectedToSlf4j {
            mainMethod.invoke(main, args)
        }
        val originalServerJar = CacheProvider.requireFile(context.serverJar)
        val targetServerJar = context.serverRoot / context.serverJar
        targetServerJar.deleteIfExists()
        originalServerJar.copyTo(targetServerJar, true)
        (context.serverRoot / "fabric-server-launcher.properties").apply {
            deleteIfExists()
            createFile()
            writer().use {
                it.write("serverJar=${context.serverJar}")
                it.flush()
            }
        }
        return "fabric-server-launch.jar"
    }

    override fun alreadyInstalledModLoader(serverRoot: Path): Boolean {
        return false
    }

    fun getLatestInstallerVersion(): String {
        try {
            val mvnXml = HttpUtil.downloadString(
                "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml",
                Charsets.UTF_8
            ).replace("\n", "").replace("\r", "")
            val matcher = versionRegex.matcher(mvnXml)
            if (!matcher.matches()) {
                throw RuntimeException("Retrieve latest installer version from fabric maven failed.")
            }
            return matcher.group(1)
        } catch (e: IORuntimeException) {
            throw RuntimeException("Retrieve latest installer version from fabric maven failed.", e)
        }
    }

    override fun fetchDownloadInfo(minecraftVersion: String, loaderVersion: String): FileMetadata {
        val latestInstallerVersion = getLatestInstallerVersion()
        val downloadUrl =
            "https://maven.fabricmc.net/net/fabricmc/fabric-installer/$latestInstallerVersion/fabric-installer-$latestInstallerVersion.jar"
        val sha1Url = "$downloadUrl.sha1"
        val sha1 = HttpUtil.get(sha1Url)
        installerDownloadInfo = FileMetadata("fabric-installer-$latestInstallerVersion.jar", downloadUrl, 0L, sha1)
        return installerDownloadInfo
    }
}