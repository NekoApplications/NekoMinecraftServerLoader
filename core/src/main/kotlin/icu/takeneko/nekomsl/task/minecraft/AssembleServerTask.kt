package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.cache.CacheProvider
import icu.takeneko.nekomsl.util.sha1
import org.apache.commons.io.FileUtils
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists

class AssembleServerTask : ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val serverRoot = context.serverRoot
        context.configureSummary.serverVersion = context.source.version
        if (context.source.modLoader != null) {
            context.configureSummary.modLoader = context.source.modLoader
            val modsDir = serverRoot / "mods"
            if (modsDir.exists()) {
                FileUtils.deleteDirectory(modsDir.toFile())
            }
            if (!modsDir.exists()) modsDir.createDirectories()
            val renameMap = buildMap {
                context.source.mods.forEach {
                    if (it.rename != null) {
                        this[it.modid] = it.rename
                    }
                }
            }
            context.modFileNameMap.forEach { (id, fileName) ->
                val modCache = CacheProvider.requireFile(fileName)
                val targetPath = modsDir / if (id in renameMap) renameMap[id]!! else fileName
                context.logger.info("Copying mod $modCache to $targetPath")
                modCache.copyTo(targetPath)
            }
        } else {
            context.logger.info("Copying server jar.")
            val serverJarCache = CacheProvider.requireFile(context.serverJar)
            val serverJar = serverRoot / context.serverJar
            if (serverJar.exists()) {
                if (serverJar.toFile().sha1() != serverJarCache.toFile().sha1()) {
                    serverJar.deleteIfExists()
                    serverJarCache.copyTo(serverJar, overwrite = true)
                }
            } else {
                serverJarCache.copyTo(serverJar, overwrite = true)
            }
        }
    }

    override fun describe(): String {
        return "AssembleServer"
    }

}
