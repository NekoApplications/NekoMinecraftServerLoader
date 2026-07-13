package icu.takeneko.nekomsl

import com.xenomachina.argparser.SystemExitException
import icu.takeneko.nekomsl.cache.CacheProvider
import icu.takeneko.nekomsl.mcversion.MinecraftVersion
import icu.takeneko.nekomsl.process.Console
import icu.takeneko.nekomsl.util.getVersionInfoString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Main")

private fun Throwable.die(message: String): Nothing {
    logger.error(message, this)
    exitProcess(1)
}

fun runNekoMSLInstance(instanceParameters: InstanceParameters) {
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
    NekoMSLInstance(instanceParameters).run()
}

fun SystemExitException.logAndExit(logger: Logger, programName: String, columns: Int): Nothing {
    val writer = StringWriter()
    printUserMessage(writer, programName, columns)
    writer.toString().trimEnd().lineSequence()
        .filter { it.isNotBlank() }
        .forEach {
            if (returnCode == 0) {
                logger.info(it)
            } else {
                logger.error(it)
            }
    }
    exitProcess(returnCode)
}
