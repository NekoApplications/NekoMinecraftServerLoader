package icu.takeneko.nekomsl

import icu.takeneko.nekomsl.cache.CacheProvider
import icu.takeneko.nekomsl.mcversion.MinecraftVersion
import icu.takeneko.nekomsl.minecraft.MinecraftServerConfig
import icu.takeneko.nekomsl.process.Console
import icu.takeneko.nekomsl.scripting.ScriptDef
import icu.takeneko.nekomsl.scripting.evalFile
import icu.takeneko.nekomsl.task.TaskRunner
import icu.takeneko.nekomsl.task.minecraft.MinecraftServerTaskScheduler
import icu.takeneko.nekomsl.task.minecraft.ServerConfigureTaskContext
import icu.takeneko.nekomsl.util.getVersionInfoString
import icu.takeneko.nekomsl.util.timer
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Path
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.onSuccess
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Main")
private val taskRunner = TaskRunner()

fun main(args: Array<String>) {
    Console.start()
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    icu.takeneko.nekomsl.logger.info("${getVersionInfoString()} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}")
    CacheProvider.init()
    try {
        icu.takeneko.nekomsl.logger.info("Updating Minecraft version cache.")
        MinecraftVersion.update()
    } catch (e: Exception) {
        icu.takeneko.nekomsl.logger.error("Update minecraft version info failed.", e)
        return
    }
    val argList = args.toList()
    var index = 0
    val file = File(
        try {
            argList[0].apply { index++ }
        } catch (_: Exception) {
            "build.server.kts"
        }
    )
    val action = try {
        argList[index].apply { index++ }
    } catch (_: Exception) {
        "runServer"
    }
    val taskArgs = args.toList().subList(index, args.size)

    if (!file.exists()) {
        icu.takeneko.nekomsl.logger.error("Server configure script $file not found.")
        return
    }
    icu.takeneko.nekomsl.logger.info("Evaluating script: $file")

    timer("Evaluate script") { evalFile(file) }.apply {
        reports.forEach {
            when (it.severity) {
                ScriptDiagnostic.Severity.DEBUG -> {
                    icu.takeneko.nekomsl.logger.debug(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.INFO -> {
                    icu.takeneko.nekomsl.logger.info(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.WARNING -> {
                    icu.takeneko.nekomsl.logger.warn(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.ERROR -> {
                    icu.takeneko.nekomsl.logger.error(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.FATAL -> {
                    icu.takeneko.nekomsl.logger.error("[FATAL] ${it.render(withSeverity = false)}")
                }
            }
        }
        this.onSuccess { result ->
            val serverConfig = (result.returnValue.scriptInstance as ScriptDef).serverConfig
            serverConfig.action = action
            serverConfig.taskArgs = taskArgs
            icu.takeneko.nekomsl.configureServer(serverConfig)
            this
        }
        this.onFailure {
            icu.takeneko.nekomsl.logger.error("Script evaluation failed with multiple errors.")
            exitProcess(1)
        }
    }
}

fun getServerDir(): Path {
    return Path.of(".", System.getProperty("serverDir") ?: "server")
}

fun configureServer(serverConfig: MinecraftServerConfig) {
    //logger.info(GsonBuilder().setPrettyPrinting().create().toJson(serverConfig))
    val taskList =
        try {
            timer("Schedule server configure tasks") { MinecraftServerTaskScheduler.schedule(serverConfig) }
        }catch (e:Exception){
            icu.takeneko.nekomsl.logger.error("Server configure tasks resolution failed with an exception.", e)
            exitProcess(1)
        }
    icu.takeneko.nekomsl.logger.info("Tasks of server configure has been scheduled.")
    taskList.forEach {
        icu.takeneko.nekomsl.logger.info("\t${it.describe()}")
    }
    icu.takeneko.nekomsl.logger.info("Executing tasks.")
    try {
        icu.takeneko.nekomsl.taskRunner.runTaskList(
            taskList,
            ServerConfigureTaskContext(MinecraftServerTaskScheduler, serverConfig, icu.takeneko.nekomsl.getServerDir())
        )
        exitProcess(0)
    } catch (e: Exception) {
        icu.takeneko.nekomsl.logger.error("Task execution failed with an exception.", e)
        exitProcess(1)
    }
}