package net.zhuruoling.nekomsl

import net.zhuruoling.nekomsl.cache.CacheProvider
import net.zhuruoling.nekomsl.mcversion.MinecraftVersion
import net.zhuruoling.nekomsl.minecraft.MinecraftServerConfig
import net.zhuruoling.nekomsl.process.Console
import net.zhuruoling.nekomsl.scripting.ScriptDef
import net.zhuruoling.nekomsl.scripting.evalFile
import net.zhuruoling.nekomsl.task.TaskRunner
import net.zhuruoling.nekomsl.task.minecraft.MinecraftServerTaskScheduler
import net.zhuruoling.nekomsl.task.minecraft.ServerConfigureTaskContext
import net.zhuruoling.nekomsl.util.getVersionInfoString
import net.zhuruoling.nekomsl.util.timer
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
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info("${getVersionInfoString()} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}")
    CacheProvider.init()
    Console.start()
    try {
        logger.info("Updating Minecraft version cache.")
        MinecraftVersion.update()
    } catch (e: Exception) {
        logger.error("Update minecraft version info failed.", e)
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