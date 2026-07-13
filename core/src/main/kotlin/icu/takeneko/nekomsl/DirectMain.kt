package icu.takeneko.nekomsl

fun main(args: Array<String>) {
    val instanceParameters = NekoMSLInstance.parseParameters(args)
    runNekoMSLInstance(instanceParameters)
}
