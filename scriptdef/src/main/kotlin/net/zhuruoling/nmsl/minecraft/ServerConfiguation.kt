package net.zhuruoling.nmsl.minecraft

import net.zhuruoling.nmsl.minecraft.mod.ModLoader
import net.zhuruoling.nmsl.minecraft.mod.ModRepository

class MinecraftServerConfig {
    var modRepositories: HashSet<ModRepository> = HashSet()
    var version: String = "latestStable"
    var modLoader: ModLoader? = null
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