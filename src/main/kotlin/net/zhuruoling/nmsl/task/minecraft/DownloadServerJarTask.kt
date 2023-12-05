package net.zhuruoling.nmsl.task.minecraft

class DownloadServerJarTask(private val version:String): ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        TODO("Not yet implemented")
    }

    override fun describe(): String {
        return "DownloadServerJar:${version}"
    }
}