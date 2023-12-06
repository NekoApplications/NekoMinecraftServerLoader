package net.zhuruoling.nekomsl.minecraft.mod.repo

import net.zhuruoling.nekomsl.minecraft.mod.loader.ModLoader

object CurseforgeModRepository: ModRepository("curseforge") {
    override fun findMod(minecraftVersion: String, modLoader: ModLoader, modId: String, modVersion: String): Pair<Mod?, List<Mod>> {
        return null to emptyList()
    }
}