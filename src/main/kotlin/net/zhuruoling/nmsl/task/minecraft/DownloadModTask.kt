package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.mod.ModItem
import net.zhuruoling.nmsl.minecraft.mod.repo.ModRepository

class DownloadModTask(val modInfo: ModItem, val modRepositories: Set<ModRepository>): ServerConfigureTask() {
    override fun invoke(context: ServerConfigureTaskContext) {
        TODO("Not yet implemented")
    }

    override fun describe(): String {
        TODO("Not yet implemented")
    }
}