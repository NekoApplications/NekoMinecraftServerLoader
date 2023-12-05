package net.zhuruoling.nmsl.minecraft.mod.loader

import net.zhuruoling.nmsl.cache.FileMetadata
import java.nio.file.Path

object ForgeModLoaderInstaller: ModLoaderInstaller("forge") {
    override fun install(installerPath: Path, path: Path, minecraftVersion: String, loaderVersion: String) {
        TODO("Not yet implemented")
    }

    override fun alreadyInstalledModLoader(serverRoot: Path): Boolean {
        TODO("Not yet implemented")
    }

    override fun fetchDownloadInfo(minecraftVersion: String, loaderVersion: String): FileMetadata {
        TODO("Not yet implemented")
    }
}