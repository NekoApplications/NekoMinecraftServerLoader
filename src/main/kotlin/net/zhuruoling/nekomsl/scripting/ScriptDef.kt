package net.zhuruoling.nekomsl.scripting

import kotlinx.coroutines.runBlocking
import net.zhuruoling.nekomsl.minecraft.MinecraftConfigurationHandlerScope
import net.zhuruoling.nekomsl.minecraft.MinecraftServerConfig
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm


@KotlinScript(
    fileExtension = "script.kts",
    compilationConfiguration = ScriptConfiguration::class
)
abstract class ScriptDef {

    val serverConfig = MinecraftServerConfig()
    val workingDir = Path(".").toAbsolutePath()
    val logger = LoggerFactory.getLogger("Script")
    fun minecraft(block: MinecraftConfigurationHandlerScope.() -> Unit) {
        MinecraftConfigurationHandlerScope(serverConfig).block()
    }

    fun procedure(id: String, block: () -> Unit) {
        serverConfig.procedures += id to block
    }
}

fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

object ScriptConfiguration : ScriptCompilationConfiguration({
    defaultImports(DependsOn::class, Repository::class)
    defaultImports("net.zhuruoling.nekomsl.minecraft.mod.*")
    defaultImports("net.zhuruoling.nekomsl.minecraft.mod.loader.*")
    defaultImports("net.zhuruoling.nekomsl.minecraft.mod.repo.*")
    defaultImports(
        "java.io.*",
        "java.nio.*",
        "java.nio.charset.*",
        "java.nio.file.*",
        "java.nio.channel.*",
        "java.lang.*",
        "java.math.*",
        "java.net.*",
        "java.util.*",
        "java.concurrent.*",
        "java.regex.*",
        "java.stream.*",
        "java.zip.*",
        "java.random.*",
        "java.function.*"
    )
    defaultImports(
        "kotlin.io.*",
        "kotlin.coroutines.*",
        "kotlin.collections.*",
        "kotlin.random.*",
        "kotlin.reflect.*",
        "kotlin.*",
        "kotlin.text.*",
        "kotlin.math.*"
    )
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    refineConfiguration {
        onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
    }
}) {
    private fun readResolve(): Any = ScriptConfiguration
}

