package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.minecraft.MinecraftServerConfig
import icu.takeneko.nekomsl.minecraft.mod.loader.ModLoader
import icu.takeneko.nekomsl.minecraft.mod.repo.Mod
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
    val configureSummary: ServerConfigurationSummary = ServerConfigurationSummary()
) : TaskContext<MinecraftServerConfig>(scheduler, source) {
    lateinit var serverJar: String
    lateinit var modLoaderInstallerPath: Path
    val modFileNameMap = mutableMapOf<String, String>()
}

data class ServerConfigurationSummary(
    var serverVersion: String = "UNDEFINED",
    var modLoader: ModLoader? = null,
    val mods: MutableList<Mod> = mutableListOf()
) {
    fun text(): String {
        return buildString {
            appendLine("Server Configuration Summary")
            appendLine()
            appendLine("Minecraft Version: $serverVersion")
            if (modLoader != null) {
                appendLine("ModLoader: ${modLoader!!.id} ${modLoader!!.version}")
                appendLine()
                appendLine("Mods:")
                for (mod in mods) {
                    appendLine("    - ${mod.modId} ${mod.version} (${mod.fileName}) (sha1:${mod.fileSha1})")
                }
            }

        }
    }
}

abstract class ServerConfigureTask : Task<MinecraftServerConfig, ServerConfigureTaskContext>() {
    override val isBlockingTask: Boolean
        get() = true
}




