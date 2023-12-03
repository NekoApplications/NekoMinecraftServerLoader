package net.zhuruoling.nmsl.minecraft.mod

import net.zhuruoling.nmsl.minecraft.mod.loader.*
import java.lang.IllegalArgumentException

class ModLoader(val id: String, val version:String, val installer: ModLoaderInstaller) {
    override fun toString(): String {
        return "ModLoader(id='$id', version='$version')"
    }
}

class ModLoaderConfigHandlerScope{
    private var id: String? = null
    var version = "latest"
    private var installer:ModLoaderInstaller? = null
    fun id(id: String){
        this.id = id
    }

    fun version(version: String){
        this.version = version
    }

    fun loader(id: String, version: String, installer: ModLoaderInstaller){
        this.id = id
        this.version = version
        this.installer = installer
    }

    fun toModLoader(): ModLoader? {
        return id?.let { ModLoader(it, version, installer ?: throw IllegalArgumentException("Unsupported mod loader: $id")) }
    }
}

val ModLoaderConfigHandlerScope.forge
    get() = loader("forge", this.version, ForgeModLoaderInstaller)
val ModLoaderConfigHandlerScope.fabric
    get() = loader("fabric", this.version, FabricModLoaderInstaller)
val ModLoaderConfigHandlerScope.neoForge
    get() = loader("neoforge", this.version, NeoForgeModLoaderInstaller)
val ModLoaderConfigHandlerScope.quilt
    get() = loader("quilt", this.version, QuiltModLoaderInstaller)