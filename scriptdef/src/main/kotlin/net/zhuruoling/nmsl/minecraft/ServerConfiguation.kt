package net.zhuruoling.nmsl.minecraft

import net.zhuruoling.nmsl.minecraft.mod.ModItem
import net.zhuruoling.nmsl.minecraft.mod.loader.ModLoader
import net.zhuruoling.nmsl.minecraft.mod.repo.ModRepository
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext

class MinecraftServerConfig {
    var taskArgs: List<String> = mutableListOf()
    var modRepositories: HashSet<ModRepository> = HashSet()
    var version: String = "latestStable"
    var modLoader: ModLoader? = null
    var mods: HashSet<ModItem> = HashSet()
    var procedures: MutableMap<String, () -> Unit>  = mutableMapOf()
    var launchConfiguration = ServerLaunchConfiguration()
    var action = "runServer"
    override fun toString(): String {
        return "MinecraftServerConfig(modRepositories=${
            modRepositories.joinToString(
                ", ",
                "[",
                "]"
            )
        }, version='$version', modLoader=$modLoader)"
    }

}

class ServerLaunchConfiguration{
    val jvmArgs: HashSet<String> = hashSetOf()
    val args: HashSet<String> = hashSetOf()
    var beforeExecutes: MutableList<String> = mutableListOf()
    var afterExecutes:MutableList<String> = mutableListOf()
}

class ServerLaunchConfigurationHandlerScope(val cfg: ServerLaunchConfiguration){

    fun jvmArgs(vararg args:String){
        cfg.jvmArgs += args
    }

    fun args(vararg args:String){
        cfg.args += args
    }

    fun before(block:RunTaskConfigurationHandlerScope.() ->Unit){
        cfg.beforeExecutes += RunTaskConfigurationHandlerScope().apply(block).list
    }

    fun after(block:RunTaskConfigurationHandlerScope.() ->Unit){
        cfg.afterExecutes += RunTaskConfigurationHandlerScope().apply(block).list
    }

    class RunTaskConfigurationHandlerScope(){

        val list: MutableList<String> = mutableListOf()

        fun execute(id:String){
            list += id
        }
    }

}