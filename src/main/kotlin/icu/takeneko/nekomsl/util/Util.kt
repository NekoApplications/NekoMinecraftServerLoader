package icu.takeneko.nekomsl.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J
import java.io.File
import java.security.MessageDigest
import java.util.*


private val logger = LoggerFactory.getLogger("Util")
val gson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
val userAgent = "NekoApplications/NekoMinecraftServerLoader/${BuildProperties["version"]}-${BuildProperties["commitId"]?.substring(0, 7)}"

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

fun File.sha1(): String {
    val messageDigest = MessageDigest.getInstance("SHA")
    val result = messageDigest.digest(this.readBytes())
    return Hex.encodeHex(result).concatToString()
}

fun findJavaExecutable():String{
    return "java"
}

fun <E> MutableList<E>.addIf(elem:E, supplier: () -> Boolean){
    if (supplier()){
        add(elem)
    }
}

fun <T> withStdoutRedirectedToSlf4j(block: () -> T):T{
    try {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
        return block()
    }catch (e:Exception){
        throw e
    }finally {
        SysOutOverSLF4J.stopSendingSystemOutAndErrToSLF4J()
    }
}