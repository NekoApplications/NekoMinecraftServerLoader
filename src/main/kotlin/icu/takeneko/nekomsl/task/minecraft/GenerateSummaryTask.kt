package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.util.getVersionInfoString
import java.lang.management.ManagementFactory
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

class GenerateSummaryTask : ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val text = buildString {
            val os = ManagementFactory.getOperatingSystemMXBean()
            val runtime = ManagementFactory.getRuntimeMXBean()
            appendLine("${getVersionInfoString()} is running on ${os.name} ${os.arch} ${os.version} at pid ${runtime.pid}.")
            appendLine()
            appendLine(context.configureSummary.text())
            appendLine()
        }
        context.serverRoot.resolve("README.txt").apply {
            deleteIfExists()
            createFile()
            writeText(text)
        }
    }

    override fun describe(): String = "GenerateConfigurationSummary"
}