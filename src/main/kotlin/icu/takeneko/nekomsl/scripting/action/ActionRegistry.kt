package icu.takeneko.nekomsl.scripting.action

import icu.takeneko.nekomsl.minecraft.MinecraftServerConfig
import icu.takeneko.nekomsl.task.Task
import icu.takeneko.nekomsl.task.TaskContext
import icu.takeneko.nekomsl.task.minecraft.BuildServerZipTask
import icu.takeneko.nekomsl.task.minecraft.RunProcedureTask
import icu.takeneko.nekomsl.task.minecraft.RunServerTask

object ActionRegistry {

    private val actions = mutableMapOf<String, ScriptAction>()

    val defaultAction: ScriptAction

    init {
        val runServer = object : ScriptAction {
            override val id: String = "runServer"

            override fun acceptTaskList(
                config: MinecraftServerConfig,
                tasks: MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>
            ) {
                addBeforeTasks(config, tasks)
                tasks.addTask(RunServerTask(config.launchConfiguration.jvmArgs, config.launchConfiguration.args))
                addAfterTasks(config, tasks)
            }
        }
        defaultAction = runServer
        register(runServer.id, runServer)
        register("buildServerZip", object : ScriptAction {
            override val id: String = "buildServerZip"

            override fun acceptTaskList(
                config: MinecraftServerConfig,
                tasks: MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>
            ) {
                addBeforeTasks(config, tasks)
                val outputFileName = config.taskArgs["output"] ?: config.taskArgs["o"] ?: "server.zip"
                tasks.addTask(BuildServerZipTask(outputFileName))
            }
        })
    }

    fun register(key: String, action: ScriptAction) {
        actions += key to action
    }

    fun resolve(key: String): ScriptAction {
        return actions[key] ?: throw IllegalArgumentException(
            "Unknown action '$key'. Available actions: ${keys().joinToString(", ")}"
        )
    }

    fun keys(): Set<String> {
        return actions.keys
    }

    private fun addBeforeTasks(
        config: MinecraftServerConfig,
        tasks: MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>
    ) {
        config.launchConfiguration.beforeExecutes.forEach {
            val procedure = config.procedures[it]
                ?: throw IllegalArgumentException("Unresolved task reference: $it")
            tasks.addTask(RunProcedureTask(it, procedure))
        }
    }

    private fun addAfterTasks(
        config: MinecraftServerConfig,
        tasks: MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>
    ) {
        config.launchConfiguration.afterExecutes.forEach {
            val procedure = config.procedures[it]
                ?: throw IllegalArgumentException("Unresolved task reference: $it")
            tasks.addTask(RunProcedureTask(it, procedure))
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>.addTask(
    element: Task<MinecraftServerConfig, *>
) {
    this.add(element as Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>)
}
