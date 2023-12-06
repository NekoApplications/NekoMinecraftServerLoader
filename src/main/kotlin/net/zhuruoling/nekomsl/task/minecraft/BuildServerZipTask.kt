package net.zhuruoling.nekomsl.task.minecraft

import cn.hutool.core.util.ZipUtil
import java.io.File
import java.io.FileFilter
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

class BuildServerZipTask(val args: List<String>) : ServerConfigureTask() {

    private val outputFileName = if (args.isEmpty()) "server.zip" else try {
        args[args.indexOf("-o") + 1]
    } catch (e: Exception) {
        "server.zip"
    }

    override fun run(context: ServerConfigureTaskContext) {
        val serverRoot = context.serverRoot
        val outputZipPath = Path(".") / outputFileName
        outputZipPath.deleteIfExists()
        ZipUtil.zip(outputZipPath.toFile(), Charsets.UTF_8, true, { pathname ->
            context.logger.info("Add: $pathname")
            true
        }, serverRoot.toFile())
    }

    override fun describe(): String {
        return "BuildServerZip[OutputFileName:$outputFileName]"
    }
}