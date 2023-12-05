package net.zhuruoling.nmsl.minecraft.mod.loader

import net.zhuruoling.nmsl.cache.FileMetadata
import java.nio.file.Path

abstract class ModLoaderInstaller(val id: String) {
    abstract fun install(installerPath: Path, path: Path, minecraftVersion: String, loaderVersion: String)

    abstract fun alreadyInstalledModLoader(serverRoot: Path):Boolean

    abstract fun fetchDownloadInfo(minecraftVersion: String, loaderVersion: String):FileMetadata
}