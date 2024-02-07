package icu.takeneko.nekomsl.minecraft.mod.loader

import icu.takeneko.nekomsl.cache.FileMetadata
import icu.takeneko.nekomsl.task.minecraft.ServerConfigureTaskContext
import java.nio.file.Path

object QuiltModLoaderInstaller: ModLoaderInstaller("quilt") {
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