package net.zhuruoling.nekomsl.minecraft.mod.loader

import net.zhuruoling.nekomsl.cache.FileMetadata
import net.zhuruoling.nekomsl.task.minecraft.ServerConfigureTaskContext
import java.nio.file.Path

object NeoForgeModLoaderInstaller: ModLoaderInstaller("neoforge") {

    override fun install(
        installerPath: Path,
        path: Path,
        minecraftVersion: String,
        loaderVersion: String,
        context: ServerConfigureTaskContext
    ): String? {
        TODO("Not yet implemented")
    }

    override fun alreadyInstalledModLoader(serverRoot: Path): Boolean {
        TODO("Not yet implemented")
    }

    override fun fetchDownloadInfo(minecraftVersion: String, loaderVersion: String): FileMetadata {
        TODO("Not yet implemented")
    }
}