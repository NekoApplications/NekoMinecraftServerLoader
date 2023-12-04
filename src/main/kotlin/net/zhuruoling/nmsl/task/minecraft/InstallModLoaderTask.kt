package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.mod.loader.ModLoader
import net.zhuruoling.nmsl.task.minecraft.ServerConfigureTask
import net.zhuruoling.nmsl.task.minecraft.ServerConfigureTaskContext

class InstallModLoaderTask(val modLoader: ModLoader): ServerConfigureTask() {
    override fun invoke(context: ServerConfigureTaskContext) {
        TODO("Not yet implemented")
    }

    override fun describe(): String {
        TODO("Not yet implemented")
    }
}