package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.mod.ModItem
import net.zhuruoling.nmsl.minecraft.mod.repo.ModRepository

class DownloadModTask(private val modInfo: ModItem, private val modRepositories: Set<ModRepository>): ServerConfigureTask() {

    override val isBlockingTask: Boolean
        get() = false

    override fun run(context: ServerConfigureTaskContext) {
        TODO("Not yet implemented")
    }

    override fun describe(): String {
        return "DownloadMod:${modInfo.toString()}:From${modRepositories.joinToString(",","[","]") { it.id }}"
    }
}