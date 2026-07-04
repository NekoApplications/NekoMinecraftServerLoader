package icu.takeneko.nekomsl.scripting.action

import icu.takeneko.nekomsl.minecraft.MinecraftServerConfig
import icu.takeneko.nekomsl.task.Task
import icu.takeneko.nekomsl.task.TaskContext

interface ScriptAction {
    val id: String

    fun acceptTaskList(
        config: MinecraftServerConfig,
        tasks: MutableList<Task<MinecraftServerConfig, TaskContext<MinecraftServerConfig>>>
    )
}
