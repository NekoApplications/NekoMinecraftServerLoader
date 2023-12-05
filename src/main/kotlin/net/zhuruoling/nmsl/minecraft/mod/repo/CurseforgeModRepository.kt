package net.zhuruoling.nmsl.minecraft.mod.repo

import net.zhuruoling.nmsl.minecraft.mod.loader.ModLoader

object CurseforgeModRepository: ModRepository("curseforge") {
    override fun findMod(minecraftVersion: String, modLoader: ModLoader, modId: String, modVersion: String): Mod? {
        return null
    }
}