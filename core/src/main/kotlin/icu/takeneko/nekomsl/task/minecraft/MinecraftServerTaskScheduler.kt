package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.minecraft.MinecraftServerConfig
import icu.takeneko.nekomsl.task.Task
import icu.takeneko.nekomsl.task.TaskContext
import icu.takeneko.nekomsl.task.TaskScheduler

object MinecraftServerTaskScheduler: TaskScheduler<MinecraftServerConfig>() {
    override fun schedule(src: MinecraftServerConfig): List<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>> {
        val result = mutableListOf<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>()
        result.addTask(PrepareEnvTask())
        result.addTask(DownloadServerJarTask(src.version))
        if (src.modLoader != null) {
            result.addTask(DownloadModLoaderInstallerTask(src.modLoader!!))
            src.mods.forEach {
                result.addTask(DownloadModTask(it, src.modRepositories))
            }
            result.addTask(InstallModLoaderTask(src.modLoader!!))
        }else{
            if (src.mods.isNotEmpty()){
                throw IllegalArgumentException("Mods are required to install but no mod loader specified!")
            }
        }
        result.addTask(AssembleServerTask())
        result.addTask(GenerateSummaryTask())
        src.action.acceptTaskList(src, result)
        return result
    }
}

@Suppress("UNCHECKED_CAST")
private fun MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>.addTask(element: Task<MinecraftServerConfig, *>) {
    this.add(element as Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>)
}
