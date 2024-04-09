package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.minecraft.mod.loader.ModLoader

class InstallModLoaderTask(val modLoader: ModLoader):ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val installer = modLoader.installer
        context.serverJar = installer.install(
            context.modLoaderInstallerPath,
            context.serverRoot,
            context.source.version,
            modLoader.version,
            context
        ) ?: return
    }

    override fun describe(): String {
        return "InstallModLoader:$modLoader]"
    }
}