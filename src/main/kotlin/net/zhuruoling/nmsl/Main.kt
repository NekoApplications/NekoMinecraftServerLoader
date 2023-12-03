package net.zhuruoling.nmsl

import net.zhuruoling.nmsl.util.BuildProperties
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

private val logger = LoggerFactory.getLogger("Main")

fun main() {
    println("Starting net.zhuruoling.omms.crystal.main.MainKt.main()")
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info("NekoMinecraftServerLoader ${BuildProperties["version"]} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}")
}