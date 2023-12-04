package net.zhuruoling.nmsl.task

abstract class TaskContext<E>(val scheduler: TaskScheduler<E>, val source: E)


abstract class Task<E, C:TaskContext<E>> {
    abstract fun invoke(context: C)

    abstract fun describe():String
}