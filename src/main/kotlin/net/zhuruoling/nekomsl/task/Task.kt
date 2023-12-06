package net.zhuruoling.nekomsl.task

abstract class TaskContext<E>(val scheduler: TaskScheduler<E>, val source: E)


abstract class Task<E, C:TaskContext<E>> {

    abstract val isBlockingTask:Boolean

    abstract fun run(context: C)

    abstract fun describe():String
}