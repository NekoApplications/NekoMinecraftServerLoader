package net.zhuruoling.nmsl.task.minecraft

class RunServerTask(val jvmArgs: HashSet<String>, val serverArgs: HashSet<String>) : ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        TODO("Not yet implemented")
    }

    override fun describe(): String {
        return "RunServer:WithJvmArgs${
            jvmArgs.joinToString(", ", "[", "]")
        }:WithServerArgs${
            serverArgs.joinToString(", ", "[", "]")
        }"
    }
}