package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.cache.CacheProvider
import net.zhuruoling.nmsl.mcversion.MinecraftVersion
import net.zhuruoling.nmsl.minecraft.mod.loader.ModLoader

class DownloadModLoaderInstallerTask(private val modLoader: ModLoader): ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val meta = modLoader.installer.fetchDownloadInfo(MinecraftVersion.parseVersion(context.source.version), modLoader.version)
        CacheProvider.downloadFile(meta)
    }

    override fun describe(): String {
        return "DownloadModLoaderInstaller:$modLoader"
    }
}