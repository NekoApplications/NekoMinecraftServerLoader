package net.zhuruoling.nmsl.minecraft.mod

import java.lang.IllegalArgumentException

data class Mod(val minecraftVersion: String, val modLoader: ModLoader, val modId: String, val downloadUrl: String)

abstract class ModRepository(val id: String) {
    abstract fun findMod(minecraftVersion: String, modLoader: ModLoader, modId: String): Mod?

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModRepository) return false

        if (id != other.id) return false

        return true
    }
}


class ModRepositoryConfigHandlerScope(var modRepositories: HashSet<ModRepository> = HashSet()) {
    fun impl(obj: ModRepository) {
        modRepositories += obj
    }

    fun id(id: String) {
        modRepositories += when (id) {
            "modrinth" -> ModrinthModRepository
            "curseforge" -> CurseforgeModRepository
            else -> throw IllegalArgumentException("Unsupported mod repository: $id")
        }
    }
}

val ModRepositoryConfigHandlerScope.modrinth
    get() = impl(ModrinthModRepository)

val ModRepositoryConfigHandlerScope.curseforge
    get() = impl(CurseforgeModRepository)