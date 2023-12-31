package net.zhuruoling.nekomsl.minecraft

import net.zhuruoling.nekomsl.minecraft.mod.ModsConfigurationHandlerScope
import net.zhuruoling.nekomsl.minecraft.mod.loader.ModLoaderConfigHandlerScope
import net.zhuruoling.nekomsl.minecraft.mod.repo.ModRepositoryConfigHandlerScope

class MinecraftConfigurationHandlerScope(private val config: MinecraftServerConfig) {

    fun version(version:String){
        config.version = version
    }

    fun modLoader(block: ModLoaderConfigHandlerScope.() -> Unit){
       config.modLoader = ModLoaderConfigHandlerScope().apply(block).toModLoader()
    }

    fun modRepository(block: ModRepositoryConfigHandlerScope.() -> Unit){
        config.modRepositories = ModRepositoryConfigHandlerScope(config.modRepositories).apply(block).modRepositories
    }

    fun mods(block: ModsConfigurationHandlerScope.() -> Unit){
        config.mods = ModsConfigurationHandlerScope(config.mods).apply(block).getMods()
    }

    fun launch(block: ServerLaunchConfigurationHandlerScope.() -> Unit){
        block(ServerLaunchConfigurationHandlerScope(config.launchConfiguration))
    }

}