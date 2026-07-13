package icu.takeneko.nekomsl

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.MissingValueException
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import icu.takeneko.nekomsl.metadata.ServerMetadata
import icu.takeneko.nekomsl.scripting.action.ActionRegistry
import icu.takeneko.nekomsl.scripting.source.StringScriptSource
import icu.takeneko.nekomsl.util.json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.system.exitProcess
import org.slf4j.LoggerFactory

private val httpClient = HttpClient.newHttpClient()
private val logger = LoggerFactory.getLogger("MetadataMain")

fun main(args: Array<String>) {
    try {
        val parsedArgs = parseMetadataCommandLine(args)
        logger.info("Loading metadata from ${parsedArgs.metadata}")
        val metadata = readMetadata(parsedArgs.metadata)
        if (metadata.metadataVersion != 0) {
            logger.error("Unsupported metadata version: ${metadata.metadataVersion}")
            exitProcess(1)
        }
        logger.info("Using instance ${parsedArgs.instance}")
        val scriptPath = metadata.servers[parsedArgs.instance]
        if (scriptPath == null) {
            logger.error(
                "Unknown instance '${parsedArgs.instance}'. Available instances: ${
                    metadata.servers.keys.joinToString(", ")
                }"
            )
            exitProcess(1)
        }
        logger.info("Ready to launch instance ${parsedArgs.instance} with action ${parsedArgs.action}")
        runNekoMSLInstance(
            InstanceParameters(
                action = ActionRegistry.resolve(parsedArgs.action),
                scriptSource = StringScriptSource(
                    source = readScript(metadata.baseUrl, scriptPath),
                    displayName = scriptPath
                ),
                taskArgs = parsedArgs.taskArgs
            )
        )
    } catch (e: SystemExitException) {
        if (e is MissingValueException) {
            printMetadataHelpAndExit()
        }
        e.logAndExit(logger, "NekoMinecraftServerLoader metadata", 120)
    } catch (e: IllegalArgumentException) {
        logger.error(e.message ?: "Invalid metadata command line arguments")
        exitProcess(1)
    }
}

private fun readMetadata(location: String): ServerMetadata {
    val metadataJson = readTextLocation(location)
    return json.decodeFromString(metadataJson)
}

private fun readScript(baseUrl: String, scriptPath: String): String {
    return if (isHttpUrl(baseUrl)) {
        readTextLocation(URI.create(ensureTrailingSlash(baseUrl)).resolve(scriptPath).toString())
    } else {
        val basePath = Path.of(baseUrl)
        val scriptFile = if (basePath.exists() && !basePath.toFile().isDirectory) {
            basePath.parent.resolve(scriptPath)
        } else {
            basePath.resolve(scriptPath)
        }
        scriptFile.readText()
    }
}

private fun readTextLocation(location: String): String {
    return if (isHttpUrl(location)) {
        val request = HttpRequest.newBuilder(URI.create(location)).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalArgumentException("Request failed for $location: HTTP ${response.statusCode()}")
        }
        response.body()
    } else {
        Path.of(location).readText()
    }
}

private fun isHttpUrl(value: String): Boolean {
    return value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)
}

private fun ensureTrailingSlash(value: String): String {
    return if (value.endsWith("/")) value else "$value/"
}

private data class MetadataCommandLine(
    val args: MetadataArgs,
    val taskArgs: Map<String, String>
) {
    val metadata: String get() = args.metadata
    val instance: String get() = args.instance
    val action: String get() = args.action
}

private class MetadataArgs(parser: ArgParser) {
    val metadata by parser.storing(
        "--metadata",
        help = "Metadata JSON URL or local file path"
    )

    val instance by parser.storing(
        "--instance",
        help = "Server key in metadata.servers"
    )

    val action by parser.storing(
        "--action",
        help = "Action to execute. Available: ${ActionRegistry.keys().joinToString(", ")}"
    ).default(ActionRegistry.defaultAction.id)
}

private fun printMetadataHelpAndExit(): Nothing {
    try {
        ArgParser(arrayOf("--help")).parseInto(::MetadataArgs)
    } catch (e: SystemExitException) {
        e.logAndExit(logger, "NekoMinecraftServerLoader metadata", 120)
    }
    error("ArgParser help did not exit")
}

private fun parseMetadataCommandLine(args: Array<String>): MetadataCommandLine {
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

            arg.startsWith("--metadata=") -> {
                parserArgs += arg
                i += 1
            }

            arg.startsWith("--instance=") -> {
                parserArgs += arg
                i += 1
            }

            arg.startsWith("--action=") -> {
                parserArgs += arg
                i += 1
            }

            arg == "--metadata" -> {
                parserArgs += arg
                if (i + 1 < args.size) {
                    parserArgs += args[i + 1]
                    i += 2
                } else {
                    i += 1
                }
            }

            arg == "--instance" -> {
                parserArgs += arg
                if (i + 1 < args.size) {
                    parserArgs += args[i + 1]
                    i += 2
                } else {
                    i += 1
                }
            }

            arg == "--action" -> {
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
    return MetadataCommandLine(
        args = ArgParser(parserArgs.toTypedArray()).parseInto(::MetadataArgs),
        taskArgs = taskArgs
    )
}
