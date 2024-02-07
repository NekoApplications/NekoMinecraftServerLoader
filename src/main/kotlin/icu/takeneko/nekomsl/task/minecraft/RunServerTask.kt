package icu.takeneko.nekomsl.task.minecraft

import icu.takeneko.nekomsl.process.Console
import icu.takeneko.nekomsl.process.ProcessDaemon
import icu.takeneko.nekomsl.util.findJavaExecutable
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread

class RunServerTask(val jvmArgs: HashSet<String>, val serverArgs: HashSet<String>) : ServerConfigureTask() {
    override fun run(context: ServerConfigureTaskContext) {
        val command =
            "${findJavaExecutable()} ${jvmArgs.joinToString(" ")} -jar ${context.serverJar} ${serverArgs.joinToString(" ")}"
        val shouldKeepWait = AtomicBoolean(true)
        val proc = ProcessDaemon(
            command,
            context.serverRoot.toAbsolutePath().toString(),
            "MinecraftServer",
            {
                println(it)
            },
            onProcessExitCallback = {
                shouldKeepWait.set(false)
            },
            onProcessErrorCallback = {
                shouldKeepWait.set(false)
            }
        )
        Console.useConsoleInput(proc::input){
            val th = thread(start = false, name = "RunServerTaskShutdownHook") {
                context.logger.info("Shutting down server because jvm is shutting down.")
                if (proc.processAlive) proc.terminate()
            }
            Runtime.getRuntime().addShutdownHook(th)
            proc.start()
            while (shouldKeepWait.get()) {
                LockSupport.parkNanos(100)
            }
            Runtime.getRuntime().removeShutdownHook(th)
        }
    }

    override fun describe(): String {
        return "RunServer:WithJvmArgs${
            jvmArgs.joinToString(", ", "[", "]")
        }:WithServerArgs${
            serverArgs.joinToString(", ", "[", "]")
        }"
    }
}