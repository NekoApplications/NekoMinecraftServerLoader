package net.zhuruoling.nmsl

import com.google.gson.GsonBuilder
import net.zhuruoling.nmsl.scripting.ScriptDef
import net.zhuruoling.nmsl.scripting.evalFile
import net.zhuruoling.nmsl.util.BuildProperties
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.onSuccess

private val logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
    println("Starting net.zhuruoling.omms.crystal.main.MainKt.main()")
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info("NekoMinecraftServerLoader ${BuildProperties["version"]} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}")
    val argList = args.toList()
    val file = File(
        try {
            argList.last()
        } catch (_: Exception) {
            "build.server.kts"
        }
    )
    if (!file.exists()) return

    evalFile(file).apply {
        reports.forEach {
            when(it.severity){
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
        this.onSuccess {
            logger.info(GsonBuilder().setPrettyPrinting().create().toJson((it.returnValue.scriptInstance as ScriptDef).serverConfig))
            this
        }
    }
}