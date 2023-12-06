package net.zhuruoling.nekomsl.task.minecraft

import net.zhuruoling.nekomsl.cache.CacheProvider
import net.zhuruoling.nekomsl.mcversion.MinecraftVersion
import net.zhuruoling.nekomsl.minecraft.mod.loader.ModLoader

class DownloadModLoaderInstallerTask(private val modLoader: ModLoader): ServerConfigureTask() {

    override val isBlockingTask: Boolean
        get() = false

    override fun run(context: ServerConfigureTaskContext) {
        val meta = modLoader.installer.fetchDownloadInfo(MinecraftVersion.parseVersion(context.source.version), modLoader.version)
        val path = CacheProvider.downloadFile(meta)
        context.modLoaderInstallerPath = path
    }

    override fun describe(): String {
        return "DownloadModLoaderInstaller:$modLoader"
    }
}