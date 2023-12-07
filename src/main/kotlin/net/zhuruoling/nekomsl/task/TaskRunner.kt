package net.zhuruoling.nekomsl.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.LockSupport

class TaskRunner {
    private val maxConcurrentTaskCount: Int = System.getenv("maxConcurrentTaskCount")?.toIntOrNull() ?: 4
    private val logger: Logger = LoggerFactory.getLogger("TaskRunner")
    private val executor: ExecutorService = Executors.newFixedThreadPool(maxConcurrentTaskCount)

    private fun withTimer(message: String, block: () -> Unit) {
        val begin = System.currentTimeMillis()
        val ret = block()
        val end = System.currentTimeMillis()
        logger.info("$message finished in ${end - begin} milliseconds.")
        return ret
    }

    fun <E> runTaskList(list: List<Task<E, TaskContext<E>>>, context: TaskContext<E>) {
        val taskStack = Stack<Task<E, TaskContext<E>>>()
        list.reversed().forEach {
            taskStack.push(it)
        }
        while (taskStack.isNotEmpty()) {
            if (taskStack.peek().isBlockingTask) {
                try {
                    val task = taskStack.pop()
                    val taskMessage = "Task: ${task.describe()}"
                    withTimer(taskMessage) {
                        logger.info(taskMessage)
                        task.run(context)
                    }
                } catch (e: Throwable) {
                    throw RuntimeException("Task execution failed: $e", e)
                }
                continue
            }
            val conTask = mutableListOf<Task<E, TaskContext<E>>>()
            while (!taskStack.peek().isBlockingTask) {
                conTask += taskStack.pop()
            }
            val futures = mutableMapOf<Future<*>, Task<*, *>>()
            conTask.forEach {
                futures += executor.submit {
                    val taskMessage = "Task: ${it.describe()}"
                    withTimer(taskMessage) {
                        logger.info(taskMessage)
                        it.run(context)
                    }
                } to it
            }
            waitTask(futures)
        }
    }

    private fun waitTask(futures: Map<Future<*>, Task<*, *>>) {
        val keys = futures.keys.toMutableList()
        val remove = mutableListOf<Future<*>>()
        val exceptions = mutableMapOf<Throwable, Task<*, *>>()
        while (keys.isNotEmpty()) {
            keys.forEach {
                if (!it.isDone) return@forEach
                try {
                    it.get()
                } catch (e: ExecutionException) {
                    exceptions += e.cause!! to futures[it]!!
                }
                remove += it
            }
            keys.removeAll(remove)
            remove.clear()
            LockSupport.parkNanos(100)
        }
        if (exceptions.isEmpty()) return
        throw RuntimeException(buildString {
            append("Task execution failed caused by multiple exceptions:")
            exceptions.forEach { (k, v) ->
                append("\nTask: ${v.describe()} : $k")
            }
        }).apply {
            exceptions.keys.forEach(this::addSuppressed)
        }
    }
}