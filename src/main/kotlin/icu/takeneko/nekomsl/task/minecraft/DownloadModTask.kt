package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.cache.CacheProvider
import icu.takeneko.nekomsl.cache.FileMetadata
import icu.takeneko.nekomsl.minecraft.mod.ModItem
import icu.takeneko.nekomsl.minecraft.mod.repo.Mod
import icu.takeneko.nekomsl.minecraft.mod.repo.ModRepository

class DownloadModTask(private val modInfo: ModItem, private val modRepositories: Set<ModRepository>) :
    ServerConfigureTask() {

    override val isBlockingTask: Boolean
        get() = false

    override fun run(context: ServerConfigureTaskContext) {
        val allCandidate = mutableMapOf<ModRepository,List<Mod>>()
        context.source.modLoader ?: run {
            context.logger.warn("No mod loader specified so no mod file will be downloaded.")
            return
        }
        val mods = modRepositories.map {
            val (result, alternative) = it.findMod(context.source.version, context.source.modLoader!!, modInfo.modid, modInfo.version)
            if (result == null){
                context.logger.warn("No mod download candidate matching [Id:${modInfo.modid}, Version:${modInfo.version}, MinecraftVersion:${context.source.version}, Loaders:${context.source.modLoader!!.installer.id}] in ${it.id}, candidates are: ${alternative.joinToString { a -> a.version }}")
                allCandidate[it] = alternative
            }
            result to it.id
        }.filter { it.first != null }.map { it.first!! to it.second }
        if (mods.isEmpty()) {
            val exceptionString = buildString {
                append("No mod download candidate matching [Id:${modInfo.modid}, Version:${modInfo.version}, MinecraftVersion:${context.source.version}, Loaders:${context.source.modLoader!!.installer.id}], ")
                append("searched in following repositories: ${modRepositories.joinToString(", ","[","]") { it.id }}")
                allCandidate.forEach { (t, u) ->
                    append("\n\tfrom \"${t.id}\", candidate versions are: ${u.joinToString { it.version }}")
                }
            }
            throw RuntimeException(exceptionString)
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