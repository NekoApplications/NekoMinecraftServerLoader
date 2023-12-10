package net.zhuruoling.nekomsl.process

import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.util.*

object Console : Thread("ConsoleThread") {
    var inputHandler: (String) -> Unit = {}
    val terminal: Terminal = TerminalBuilder.builder().system(true).dumb(true).build()
    fun <T> useConsoleInput(handler: (String) -> Unit, block: () -> T): T {
        return synchronized(this) {
            inputHandler = handler
            val t = block()
            inputHandler = {}
            t
        }
    }

    private fun Scanner.lines() = sequence {
        while (hasNext()) {
            yield(readlnOrNull())
        }
    }

    override fun run() {
        val scanner = Scanner(System.`in`)
        while (scanner.hasNext()) {
            val line = scanner.nextLine()
            inputHandler(line)
        }
    }
}