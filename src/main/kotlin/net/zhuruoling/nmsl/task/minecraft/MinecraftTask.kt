package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.MinecraftServerConfig
import net.zhuruoling.nmsl.task.Task
import net.zhuruoling.nmsl.task.TaskContext
import net.zhuruoling.nmsl.task.TaskScheduler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.System.LoggerFinder
import java.nio.file.Path

class ServerConfigureTaskContext(
    scheduler: TaskScheduler<MinecraftServerConfig>,
    source: MinecraftServerConfig,
    val serverRoot: Path,
    val logger: Logger = LoggerFactory.getLogger("ServerConfigure")
) : TaskContext<MinecraftServerConfig>(scheduler, source)

abstract class ServerConfigureTask: Task<MinecraftServerConfig, ServerConfigureTaskContext>(){
    override val isBlockingTask: Boolean
        get() = true
}




