package net.zhuruoling.nekomsl.task.minecraft

import cn.hutool.core.bean.copier.CopyOptions
import cn.hutool.core.io.FileUtil
import net.zhuruoling.nekomsl.cache.CacheProvider
import net.zhuruoling.nekomsl.util.sha1
import org.apache.commons.io.FileUtils
import java.nio.file.CopyOption
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

class AssembleServerTask : ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val serverRoot = context.serverRoot
        if (context.source.modLoader != null) {
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
                    serverJar.createFile()
                    FileUtil.copy(serverJarCache, serverJar, StandardCopyOption.REPLACE_EXISTING)
                }
            } else {
                serverJar.deleteIfExists()
                serverJar.createFile()
                FileUtil.copy(serverJarCache, serverJar, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    override fun describe(): String {
        return "AssembleServer"
    }

}