package net.zhuruoling.nmsl.minecraft.mod

data class ModItem(val modid: String, val version: String, val rename: String?) {
    override fun toString(): String {
        return ("$modid[Version:$version") + if (rename != null)
            ", Rename:$rename]"
        else "]"
    }
}

class ModsConfigurationHandlerScope(private val set: HashSet<ModItem>) {

    val scopes: HashSet<ModItemConfigurationHandlerScope> = HashSet()

    fun modid(id: String): ModItemConfigurationHandlerScope {
        return ModItemConfigurationHandlerScope(id, this)
    }

    fun modid(id: String, block: ModItemConfigurationHandlerScope.() -> Unit): ModItemConfigurationHandlerScope {
        return ModItemConfigurationHandlerScope(id, this).apply(block)
    }

    fun getMods(): HashSet<ModItem> {
        val s = scopes.map { it.toModItem() }.toHashSet()
        return set.apply { addAll(s) }
    }
}

class ModItemConfigurationHandlerScope(private val modid: String, private val parent: ModsConfigurationHandlerScope) {
    var version = "latest"
    private var rename: String? = null

    init {
        parent.scopes += this
    }

    fun rename(name: String) {
        rename = name
    }

    fun version(v: String) {
        version = v
    }

    fun toModItem(): ModItem {
        return ModItem(modid, version, rename)
    }

    operator fun invoke(block: ModItemConfigurationHandlerScope.() -> Unit) {
        this.block()
    }

    override fun hashCode(): Int {
        return modid.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModItemConfigurationHandlerScope) return false

        if (modid != other.modid) return false
        if (parent != other.parent) return false

        return true
    }
}

infix fun ModItemConfigurationHandlerScope.version(version: String) {
    this.version = version
}