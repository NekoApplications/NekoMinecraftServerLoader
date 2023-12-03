package net.zhuruoling.nmsl.minecraft.mod.loader

import java.nio.file.Path

abstract class ModLoaderInstaller(val id:String) {
    abstract fun install(path:Path, minecraftVersion:String, loaderVersion:String)
}