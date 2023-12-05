package net.zhuruoling.nmsl

import net.zhuruoling.nmsl.minecraft.MinecraftServerConfig
import net.zhuruoling.nmsl.scripting.ScriptDef
import net.zhuruoling.nmsl.scripting.evalFile
import net.zhuruoling.nmsl.task.TaskRunner
import net.zhuruoling.nmsl.task.minecraft.MinecraftServerTaskScheduler
import net.zhuruoling.nmsl.util.getVersionInfoString
import net.zhuruoling.nmsl.util.timer
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Path
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.onSuccess

private val logger = LoggerFactory.getLogger("Main")
private val taskRunner = TaskRunner()

fun main(args: Array<String>) {
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info("${getVersionInfoString()} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}")
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
    val taskArgs = args.toList().subList(index,args.size)

    if (!file.exists()) {
        logger.error("Cannot find server configure script: $file")
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
            logger.error("Server configure resolution has failed!")
        }
    }
}

fun getServerDir(): Path {
    return Path.of(".", System.getProperty("serverDir") ?: "server")
}

fun configureServer(serverConfig: MinecraftServerConfig) {
    //logger.info(GsonBuilder().setPrettyPrinting().create().toJson(serverConfig))
    val taskList = timer("Schedule server configure resolution") { MinecraftServerTaskScheduler.schedule(serverConfig) }
    logger.info("A server configure resolution has been determined.")
    taskList.forEach {
        logger.info("\t${it.describe()}")
    }
    //taskRunner.runTaskList(taskList, ServerConfigureTaskContext(MinecraftServerTaskScheduler, serverConfig, getServerDir()))
}