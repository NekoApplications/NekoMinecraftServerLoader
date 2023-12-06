package net.zhuruoling.nekomsl.task.minecraft

import net.zhuruoling.nekomsl.cache.CacheProvider
import net.zhuruoling.nekomsl.cache.FileMetadata
import net.zhuruoling.nekomsl.minecraft.mod.ModItem
import net.zhuruoling.nekomsl.minecraft.mod.repo.ModRepository

class DownloadModTask(private val modInfo: ModItem, private val modRepositories: Set<ModRepository>) :
    ServerConfigureTask() {

    override val isBlockingTask: Boolean
        get() = false

    override fun run(context: ServerConfigureTaskContext) {
        context.source.modLoader ?: run {
            context.logger.warn("No mod loader specified so no mod file will be downloaded.")
            return
        }
        val mods = modRepositories.map {
            val (result, alternative) = it.findMod(context.source.version, context.source.modLoader!!, modInfo.modid, modInfo.version)
            if (result == null){
                context.logger.warn("No mod download candidate matching [Id:${modInfo.modid}, Version:${modInfo.version}, MinecraftVersion:${context.source.version}, Loaders:${context.source.modLoader!!.installer.id}] in ${it.id}, candidates are: ${alternative.joinToString { a -> a.version }}")
            }
            result to it.id
        }.filter { it.first != null }.map { it.first!! to it.second }
        if (mods.isEmpty()) {
            throw RuntimeException("No mod download candidate matching [Id:${modInfo.modid}, Version:${modInfo.version}, MinecraftVersion:${context.source.version}, Loaders:${context.source.modLoader!!.installer.id}], searched in following repositories: ${modRepositories.joinToString(", ","[","]") { it.id }}")
        }
        val (mod,repoId)= mods.first()
        context.logger.info("Found mod ${mod.modId} download url from $repoId.")
        val downloadInfo = FileMetadata(mod.fileName, mod.downloadUrl, mod.fileSize, mod.fileSha1)
        CacheProvider.downloadFile(downloadInfo)
        context.modFileNameMap[modInfo.modid] = downloadInfo.fileName
    }

    override fun describe(): String {
        return "DownloadMod:$modInfo:From${modRepositories.joinToString(",", "[", "]") { it.id }}"
    }
}