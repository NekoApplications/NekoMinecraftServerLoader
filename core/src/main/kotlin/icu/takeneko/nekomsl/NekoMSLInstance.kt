package icu.takeneko.nekomsl

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import icu.takeneko.nekomsl.minecraft.MinecraftServerConfig
import icu.takeneko.nekomsl.scripting.ScriptDef
import icu.takeneko.nekomsl.scripting.action.ActionRegistry
import icu.takeneko.nekomsl.scripting.action.ScriptAction
import icu.takeneko.nekomsl.scripting.source.FileScriptSource
import icu.takeneko.nekomsl.scripting.source.ScriptSource
import icu.takeneko.nekomsl.task.TaskRunner
import icu.takeneko.nekomsl.task.minecraft.MinecraftServerTaskScheduler
import icu.takeneko.nekomsl.task.minecraft.ServerConfigureTaskContext
import icu.takeneko.nekomsl.util.timer
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.onSuccess
import kotlin.system.exitProcess

data class InstanceParameters(
    val action: ScriptAction,
    val scriptSource: ScriptSource,
    val taskArgs: Map<String, String>
)

class NekoMSLInstance(private val parameters: InstanceParameters) {

    private val logger = LoggerFactory.getLogger("Instance")
    private val taskRunner = TaskRunner()

    fun run() {
        val scriptSource = parameters.scriptSource
        logger.info("Evaluating script: ${scriptSource.displayName}")

        val evalResult = try {
            timer("Evaluate script") { scriptSource.eval() }
        } catch (e: Exception) {
            logger.error("Script loading failed.", e)
            exitProcess(1)
        }
        evalResult.apply {
            reports.forEach {
                when (it.severity) {
                    ScriptDiagnostic.Severity.DEBUG -> logger.debug(it.render(withSeverity = false))
                    ScriptDiagnostic.Severity.INFO -> logger.info(it.render(withSeverity = false))
                    ScriptDiagnostic.Severity.WARNING -> logger.warn(it.render(withSeverity = false))
                    ScriptDiagnostic.Severity.ERROR -> logger.error(it.render(withSeverity = false))
                    ScriptDiagnostic.Severity.FATAL -> logger.error("[FATAL] ${it.render(withSeverity = false)}")
                }
            }
            this.onSuccess { result ->
                val serverConfig = (result.returnValue.scriptInstance as ScriptDef).serverConfig
                serverConfig.action = parameters.action
                serverConfig.taskArgs = parameters.taskArgs
                configureServer(serverConfig)
                this
            }
            this.onFailure {
                logger.error("Script evaluation failed with multiple errors.")
                exitProcess(1)
            }
        }
    }

    private fun getServerDir(): Path {
        return Path.of(".", System.getProperty("serverDir") ?: "server")
    }

    private fun configureServer(serverConfig: MinecraftServerConfig) {
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

    companion object {
        private val logger = LoggerFactory.getLogger("CommandLine")

        fun parseParameters(args: Array<String>): InstanceParameters {
            return try {
                parseDirectCommandLine(args).toInstanceParameters()
            } catch (e: SystemExitException) {
                e.logAndExit(logger, "NekoMinecraftServerLoader", 120)
            } catch (e: IllegalArgumentException) {
                logger.error(e.message ?: "Invalid command line arguments")
                exitProcess(1)
            }
        }
    }
}

private data class DirectCommandLine(
    val args: DirectArgs,
    val taskArgs: Map<String, String>
) {
    fun toInstanceParameters(): InstanceParameters {
        if (args.action.isEmpty()) {
            ArgParser(arrayOf("--help")).parseInto(::DirectArgs)
        }
        return InstanceParameters(
            action = ActionRegistry.resolve(args.action),
            scriptSource = FileScriptSource(File(args.script)),
            taskArgs = taskArgs
        )
    }
}

private class DirectArgs(parser: ArgParser) {
    val action by parser.storing(
        "-a",
        "--action",
        help = "Action to execute. Available: ${ActionRegistry.keys().joinToString(", ")}"
    ).default("")

    val script by parser.storing(
        "-s",
        "--script",
        help = "Path to a .server.kts script file"
    ).default("build.server.kts")
}

private fun parseDirectCommandLine(args: Array<String>): DirectCommandLine {
    val parserArgs = mutableListOf<String>()
    val taskArgs = linkedMapOf<String, String>()
    var positionalIndex = 0
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg == "--help" || arg == "-h" || arg == "help" -> {
                parserArgs += arg
                i += 1
            }

            arg.startsWith("--action=") -> {
                parserArgs += arg
                i += 1
            }

            arg.startsWith("--script=") -> {
                parserArgs += arg
                i += 1
            }

            arg == "--action" || arg == "-a" -> {
                parserArgs += arg
                if (i + 1 < args.size) {
                    parserArgs += args[i + 1]
                    i += 2
                } else {
                    i += 1
                }
            }

            arg == "--script" || arg == "-s" -> {
                parserArgs += arg
                if (i + 1 < args.size) {
                    parserArgs += args[i + 1]
                    i += 2
                } else {
                    i += 1
                }
            }

            arg.startsWith("--") && "=" in arg -> {
                val key = arg.substringBefore("=").removePrefix("--")
                taskArgs[key] = arg.substringAfter("=")
                i += 1
            }

            arg.startsWith("--") -> {
                val key = arg.removePrefix("--")
                if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                    taskArgs[key] = args[i + 1]
                    i += 2
                } else {
                    taskArgs[key] = "true"
                    i += 1
                }
            }

            arg.startsWith("-") && "=" in arg -> {
                val key = arg.substringBefore("=").removePrefix("-")
                taskArgs[key] = arg.substringAfter("=")
                i += 1
            }

            arg.startsWith("-") && arg.length > 1 -> {
                val key = arg.removePrefix("-")
                if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                    taskArgs[key] = args[i + 1]
                    i += 2
                } else {
                    taskArgs[key] = "true"
                    i += 1
                }
            }

            else -> {
                taskArgs["_$positionalIndex"] = arg
                positionalIndex += 1
                i += 1
            }
        }
    }
    return DirectCommandLine(
        args = ArgParser(parserArgs.toTypedArray()).parseInto(::DirectArgs),
        taskArgs = taskArgs
    )
}
