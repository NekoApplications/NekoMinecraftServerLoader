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

private fun Throwable.die(message: String):Nothing {
    logger.error(message, this)
    exitProcess(1)
}

private fun Throwable.die():Nothing {
    logger.error(this.message, this)
    exitProcess(1)
}

fun main(args: Array<String>) {
    Console.start()
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info("${getVersionInfoString()} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}")
    CacheProvider.init()
    try {
        logger.info("Updating Minecraft version cache.")
        MinecraftVersion.update()
    } catch (e: Exception) {
        e.die("Update minecraft version info failed.")
    }
    val argList = args.toList()
    val file = try {
        File(".").listFiles()?.first { it.isFile && it.toString().endsWith(".server.kts") }
            ?: RuntimeException("No configuration script found.").die()
    } catch (e: Throwable) {
        e.die("No configuration script found.")
    }
    val action = argList.firstOrNull() ?: "runServer"
    val taskArgs = args.toList().subList(1, args.size)


    if (!file.exists()) {
        logger.error("Server configure script $file not found.")
        return
    }
    logger.info("Evaluating script: $file")

    timer("Evaluate script") { evalFile(file) }.apply {
        reports.forEach {
            when (it.severity) {
                ScriptDiagnostic.Severity.DEBUG -> {
                    logger.debug(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.INFO -> {
                    logger.info(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.WARNING -> {
                    logger.warn(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.ERROR -> {
                    logger.error(it.render(withSeverity = false))
                }

                ScriptDiagnostic.Severity.FATAL -> {
                    logger.error("[FATAL] ${it.render(withSeverity = false)}")
                }
            }
        }
        this.onSuccess { result ->
            val serverConfig = (result.returnValue.scriptInstance as ScriptDef).serverConfig
            serverConfig.action = action
            serverConfig.taskArgs = taskArgs
            configureServer(serverConfig)
            this
        }
        this.onFailure {
            logger.error("Script evaluation failed with multiple errors.")
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
        } catch (e: Exception) {
            logger.error("Server configure tasks resolution failed with an exception.", e)
            exitProcess(1)
        }
    logger.info("Tasks of server configure has been scheduled.")
    taskList.forEach {
        logger.info("\t${it.describe()}")
    }
    logger.info("Executing tasks.")
    try {
        taskRunner.runTaskList(
            taskList,
            ServerConfigureTaskContext(MinecraftServerTaskScheduler, serverConfig, getServerDir())
        )
        exitProcess(0)
    } catch (e: Exception) {
        logger.error("Task execution failed with an exception.", e)
        exitProcess(1)
    }
}