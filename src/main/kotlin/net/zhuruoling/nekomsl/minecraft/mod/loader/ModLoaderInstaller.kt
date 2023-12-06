package net.zhuruoling.nekomsl.minecraft.mod.loader

import net.zhuruoling.nekomsl.cache.FileMetadata
import net.zhuruoling.nekomsl.task.minecraft.ServerConfigureTaskContext
import java.nio.file.Path

abstract class ModLoaderInstaller(val id: String) {
    abstract fun install(
        installerPath: Path,
        path: Path,
        minecraftVersion: String,
        loaderVersion: String,
        context: ServerConfigureTaskContext
    ): String?

    abstract fun alreadyInstalledModLoader(serverRoot: Path):Boolean

    abstract fun fetchDownloadInfo(minecraftVersion: String, loaderVersion: String):FileMetadata
}