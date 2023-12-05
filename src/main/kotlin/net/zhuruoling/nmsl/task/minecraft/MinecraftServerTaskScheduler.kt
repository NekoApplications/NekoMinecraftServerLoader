package net.zhuruoling.nmsl.task.minecraft

import net.zhuruoling.nmsl.minecraft.MinecraftServerConfig
import net.zhuruoling.nmsl.task.Task
import net.zhuruoling.nmsl.task.TaskContext
import net.zhuruoling.nmsl.task.TaskScheduler

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
        }
        when(src.action){
            "runServer" -> {
                src.launchConfiguration.beforeExecutes.forEach {
                    result.addTask(RunProcedureTask(it, src.procedures[it]!!))
                }
                result.addTask(RunServerTask(src.launchConfiguration.jvmArgs, src.launchConfiguration.args))
                src.launchConfiguration.afterExecutes.forEach {
                    result.addTask(RunProcedureTask(it, src.procedures[it]!!))
                }
            }
            "buildServerZip" -> {
                src.launchConfiguration.beforeExecutes.forEach {
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
