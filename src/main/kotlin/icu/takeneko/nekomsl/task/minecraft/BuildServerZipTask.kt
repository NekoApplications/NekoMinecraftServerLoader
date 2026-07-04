package icu.takeneko.nekomsl.task.minecraft

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

class BuildServerZipTask(private val outputFileName: String) : ServerConfigureTask() {

    override fun run(context: ServerConfigureTaskContext) {
        val serverRoot = context.serverRoot
        val outputZipPath = Path(".") / outputFileName
        outputZipPath.deleteIfExists()
        ZipOutputStream(outputZipPath.outputStream(), Charsets.UTF_8).use { zip ->
            Files.walk(serverRoot).use { paths ->
                paths.filter { it != serverRoot }.forEach {
                    val entryName = serverRoot.relativize(it).toString().replace('\\', '/')
                    context.logger.info("Add: $it")
                    if (it.isDirectory()) {
                        zip.putNextEntry(ZipEntry("$entryName/"))
                        zip.closeEntry()
                    } else {
                        zip.putNextEntry(ZipEntry(entryName))
                        Files.copy(it, zip)
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    override fun describe(): String {
        return "BuildServerZip[OutputFileName:$outputFileName]"
    }
}
