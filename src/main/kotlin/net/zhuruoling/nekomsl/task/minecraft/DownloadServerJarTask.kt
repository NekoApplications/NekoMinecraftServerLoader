package net.zhuruoling.nekomsl.task.minecraft

import net.zhuruoling.nekomsl.cache.CacheProvider
import net.zhuruoling.nekomsl.mcversion.MinecraftVersion

class DownloadServerJarTask(private val version:String): ServerConfigureTask() {

    override val isBlockingTask: Boolean
        get() = false

    override fun run(context: ServerConfigureTaskContext) {
        val serverJarMeta = MinecraftVersion.resolveVersionDataForDownload(version)
        context.logger.info("Downloading Minecraft Server $version Jar.")
        CacheProvider.downloadFile(serverJarMeta)
        context.serverJar = serverJarMeta.fileName
    }

    override fun describe(): String {
        return "DownloadServerJar:${version}"
    }
}