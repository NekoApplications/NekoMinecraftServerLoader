package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.MinecraftServerConfig
import net.zhuruoling.nmsl.task.Task
import net.zhuruoling.nmsl.task.TaskContext
import net.zhuruoling.nmsl.task.TaskScheduler
import java.nio.file.Path

abstract class ServerConfigureTaskContext(
    scheduler: TaskScheduler<MinecraftServerConfig>,
    source: MinecraftServerConfig,
    val serverRoot: Path
) : TaskContext<MinecraftServerConfig>(scheduler, source)

abstract class ServerConfigureTask: Task<MinecraftServerConfig, ServerConfigureTaskContext>()




