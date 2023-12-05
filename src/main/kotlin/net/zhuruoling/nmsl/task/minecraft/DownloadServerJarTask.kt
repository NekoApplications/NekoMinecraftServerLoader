package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.cache.CacheProvider
import net.zhuruoling.nmsl.mcversion.MinecraftVersion

class DownloadServerJarTask(private val version:String): ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val serverJarMeta = MinecraftVersion.resolveVersionDataForDownload(version)
        context.logger.info("Downloading Minecraft Server $version Jar.")
        CacheProvider.downloadFile(serverJarMeta)
    }

    override fun describe(): String {
        return "DownloadServerJar:${version}"
    }
}