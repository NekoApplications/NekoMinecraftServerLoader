package net.zhuruoling.nmsl.task

abstract class TaskScheduler<E>{
    abstract fun schedule(src: E):List<Task<E, TaskContext<E>>>
}

object Schedulers{

}