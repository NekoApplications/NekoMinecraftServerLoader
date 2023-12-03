package net.zhuruoling.nmsl.scripting

import kotlinx.coroutines.runBlocking
import net.zhuruoling.nmsl.minecraft.MinecraftConfigurationHandlerScope
import net.zhuruoling.nmsl.minecraft.MinecraftServerConfig
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
abstract class ScriptDef{

    val serverConfig = MinecraftServerConfig()

    fun minecraft(block: MinecraftConfigurationHandlerScope.() -> Unit){
        MinecraftConfigurationHandlerScope(serverConfig).block()
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

object ScriptConfiguration: ScriptCompilationConfiguration({
    defaultImports(DependsOn::class, Repository::class)
    defaultImports("net.zhuruoling.nmsl.minecraft.mod.*")
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    refineConfiguration {
        onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
    }
}) {
    private fun readResolve(): Any = ScriptConfiguration
}

