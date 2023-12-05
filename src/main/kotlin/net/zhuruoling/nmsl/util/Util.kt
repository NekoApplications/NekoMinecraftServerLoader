package net.zhuruoling.nmsl.util

import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("Util")

fun <T> timer(message: String, block: () -> T): T {
    val begin = System.currentTimeMillis()
    val ret = block()
    val end = System.currentTimeMillis()
    logger.info("$message finished in ${end - begin} milliseconds.")
    return ret
}

fun getVersionInfoString(): String {
    val version = BuildProperties["version"]
    val buildTimeMillis = BuildProperties["buildTime"]?.toLong() ?: 0L
    val buildTime = Date(buildTimeMillis)
    return "NekoApplications::NekoMinecraftServerLoader $version (${BuildProperties["branch"]}:${
        BuildProperties["commitId"]?.substring(0, 7)
    } $buildTime)"
}