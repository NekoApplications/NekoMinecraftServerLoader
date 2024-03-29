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
        when(src.action){
            "runServer" -> {
                src.launchConfiguration.beforeExecutes.forEach {
                    if (it !in src.launchConfiguration.beforeExecutes)
                        throw IllegalArgumentException("Unresolved task reference: $it")
                    result.addTask(RunProcedureTask(it, src.procedures[it]!!))
                }
                result.addTask(RunServerTask(src.launchConfiguration.jvmArgs, src.launchConfiguration.args))
                src.launchConfiguration.afterExecutes.forEach {
                    if (it !in src.launchConfiguration.afterExecutes)
                        throw IllegalArgumentException("Unresolved task reference: $it")
                    result.addTask(RunProcedureTask(it, src.procedures[it]!!))
                }
            }
            "buildServerZip" -> {
                src.launchConfiguration.beforeExecutes.forEach {
                    if (it !in src.launchConfiguration.beforeExecutes)
                        throw IllegalArgumentException("Unresolved task reference: $it")
                    result.addTask(RunProcedureTask(it, src.procedures[it]!!))
                }
                result.addTask(BuildServerZipTask(src.taskArgs))
            }
            else -> {}
        }
        return result
    }
}

@Suppress("UNCHECKED_CAST")
private fun MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>.addTask(element: Task<MinecraftServerConfig, *>) {
    this.add(element as Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>)
}
