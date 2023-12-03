package net.zhuruoling.nmsl.minecraft

import net.zhuruoling.nmsl.minecraft.mod.ModLoaderConfigHandlerScope
import net.zhuruoling.nmsl.minecraft.mod.ModRepositoryConfigHandlerScope

class MinecraftConfigurationHandlerScope(private val config: MinecraftServerConfig) {

    fun version(version:String){
        config.version = version
    }

    fun modLoader(block: ModLoaderConfigHandlerScope.() -> Unit){
       config.modLoader = ModLoaderConfigHandlerScope().apply(block).toModLoader()
    }

    fun modRepository(block: ModRepositoryConfigHandlerScope.() ->Unit){
        config.modRepositories = ModRepositoryConfigHandlerScope(config.modRepositories).apply(block).modRepositories
    }
}