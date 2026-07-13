package icu.takeneko.nekomsl.scripting.source

import icu.takeneko.nekomsl.scripting.ScriptDef
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.StringScriptSource as KotlinStringScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

abstract class ScriptSource {
    abstract val displayName: String

    protected abstract fun sourceCode(): SourceCode

    open fun validate() {
    }

    fun eval(): ResultWithDiagnostics<EvaluationResult> {
        validate()
        return BasicJvmScriptingHost().eval(sourceCode(), compilationConfiguration, evalConfig)
    }

    private companion object {
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptDef>()
        val evalConfig = ScriptEvaluationConfiguration {}
    }
}

class FileScriptSource(private val file: File) : ScriptSource() {
    override val displayName: String = file.toString()

    override fun validate() {
        if (!file.exists()) {
            throw IllegalArgumentException("Server configure script $file not found.")
        }
    }

    override fun sourceCode(): SourceCode {
        return file.toScriptSource()
    }
}

class StringScriptSource(
    private val source: String,
    override val displayName: String = "inline.server.kts"
) : ScriptSource() {

    override fun sourceCode(): SourceCode {
        return KotlinStringScriptSource(source, displayName, displayName)
    }
}
