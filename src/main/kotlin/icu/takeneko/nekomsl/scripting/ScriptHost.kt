package icu.takeneko.nekomsl.scripting

import icu.takeneko.nekomsl.scripting.source.FileScriptSource
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
    return FileScriptSource(scriptFile).eval()
}
