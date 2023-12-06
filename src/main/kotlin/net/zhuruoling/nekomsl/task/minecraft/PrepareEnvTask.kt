package net.zhuruoling.nekomsl.task.minecraft

import org.apache.commons.io.FileUtils
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class PrepareEnvTask : ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val root = context.serverRoot
        if (root.notExists()){
            root.createDirectories()
        }
    }

    override fun describe(): String {
        return "PrepareServerEnvironment"
    }
}