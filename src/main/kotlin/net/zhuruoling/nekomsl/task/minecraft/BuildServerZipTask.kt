package net.zhuruoling.nekomsl.task.minecraft

class BuildServerZipTask(val args: List<String>) : ServerConfigureTask() {

    private val outputFileName = if (args.isEmpty()) "server.zip" else try {
        args[args.indexOf("-o") + 1]
    } catch (e: Exception) {
        "server.zip"
    }

    override fun run(context: ServerConfigureTaskContext) {

    }

    override fun describe(): String {
        return "BuildServerZip[OutputFileName:$outputFileName]"
    }
}