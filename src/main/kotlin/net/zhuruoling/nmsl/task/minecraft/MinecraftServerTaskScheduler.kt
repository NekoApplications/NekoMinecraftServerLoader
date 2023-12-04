package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.MinecraftServerConfig
import net.zhuruoling.nmsl.task.Task
import net.zhuruoling.nmsl.task.TaskContext
import net.zhuruoling.nmsl.task.TaskScheduler

object MinecraftServerTaskScheduler: TaskScheduler<MinecraftServerConfig>() {
    override fun schedule(src: MinecraftServerConfig): List<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>> {
        val result = mutableListOf<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>()
        

        return result
    }
}