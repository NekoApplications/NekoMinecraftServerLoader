package net.zhuruoling.nekomsl.task.minecraft

import net.zhuruoling.nekomsl.minecraft.MinecraftServerConfig
import net.zhuruoling.nekomsl.task.Task
import net.zhuruoling.nekomsl.task.TaskContext
import net.zhuruoling.nekomsl.task.TaskScheduler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.System.LoggerFinder
import java.nio.file.Path

class ServerConfigureTaskContext(
    scheduler: TaskScheduler<MinecraftServerConfig>,
    source: MinecraftServerConfig,
    val serverRoot: Path,
    val logger: Logger = LoggerFactory.getLogger("ServerConfigure"),
) : TaskContext<MinecraftServerConfig>(scheduler, source){
    lateinit var serverJar: String
    lateinit var modLoaderInstallerPath:Path
    val modFileNameMap = mutableMapOf<String, String>()
}

abstract class ServerConfigureTask: Task<MinecraftServerConfig, ServerConfigureTaskContext>(){
    override val isBlockingTask: Boolean
        get() = true
}




