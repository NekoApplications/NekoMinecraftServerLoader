package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.mod.loader.ModLoader

class InstallModLoaderTask(val modLoader: ModLoader):ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        TODO("Not yet implemented")
    }

    override fun describe(): String {
        return "InstallModLoader:$modLoader]"
    }
}