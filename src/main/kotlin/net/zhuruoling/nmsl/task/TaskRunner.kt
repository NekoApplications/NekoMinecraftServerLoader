package net.zhuruoling.nmsl.task

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

    val logger: Logger = LoggerFactory.getLogger("TaskRunner")
    val executor: ExecutorService = Executors.newFixedThreadPool(maxConcurrentTaskCount)

    inline fun <reified E> runTaskList(list: List<Task<E, TaskContext<E>>>, context: TaskContext<E>) {
        val taskStack = Stack<Task<E, TaskContext<E>>>()
        list.reversed().forEach {
            taskStack.push(it)
        }
        while (taskStack.isNotEmpty()) {
            if (taskStack.peek().isBlockingTask) {
                taskStack.pop().apply {
                    logger.info("Task: ${describe()}")
                    run(context)
                }
            } else {
                val conTask = mutableListOf<Task<E, TaskContext<E>>>()
                while (!taskStack.peek().isBlockingTask) {
                    conTask += taskStack.pop()
                }
                val futures = mutableMapOf<Future<*>, Task<*, *>>()
                conTask.forEach {
                    futures += executor.submit {
                        it.apply {
                            logger.info("Task: ${describe()}")
                            run(context)
                        }
                    } to it
                }
                waitTask(futures)
            }
        }
    }

    fun waitTask(futures: Map<Future<*>, Task<*, *>>) {
        val keys = futures.keys.toMutableList()
        val remove = mutableListOf<Future<*>>()
        val result = mutableMapOf<Throwable, Task<*,*>>()
        while (futures.isNotEmpty()) {
            keys.forEach {
                if (it.isDone) {
                    try {
                        it.get()
                    } catch (e: ExecutionException) {
                        result += e.cause!! to futures[it]!!
                    }
                    remove += it
                }
            }
            keys.removeAll(remove)
            remove.clear()
            LockSupport.parkNanos(100)
        }
        if (result.isNotEmpty()) {
            throw RuntimeException(buildString {
                append("Task execution failed caused by multiple exceptions:\n")
                result.forEach { k, v ->
                    append("Task: ${v.describe()} : $k")
                }
            }).apply {
                result.keys.forEach(this::addSuppressed)
            }
        }
    }
}