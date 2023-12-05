package net.zhuruoling.nmsl.task.minecraft

class RunProcedureTask(private val procedureId:String, val block: () ->Unit): ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        context.logger.info("Run: $procedureId")
        block()
    }

    override fun describe(): String {
        return "RunProcedure:$procedureId"
    }
}