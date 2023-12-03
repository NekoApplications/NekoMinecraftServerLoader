package net.zhuruoling.nmsl.minecraft.mod

object ModrinthModRepository:ModRepository("modrinth") {
    override fun findMod(minecraftVersion: String, modLoader: ModLoader, modId: String): Mod? {
        TODO("Not yet implemented")
    }
}