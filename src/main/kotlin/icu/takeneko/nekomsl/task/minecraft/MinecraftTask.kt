package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.minecraft.MinecraftServerConfig
import icu.takeneko.nekomsl.task.Task
import icu.takeneko.nekomsl.task.TaskContext
import icu.takeneko.nekomsl.task.TaskScheduler
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




