package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.Position

internal interface DiagnosticsParser {
  fun parse(
    bazelOutputLines: List<String>,
    target: Label,
    isCommandLineFormattedOutput: Boolean = false,
    onlyFromParsedOutput: Boolean = false,
  ): List<Diagnostic>
}

internal class DiagnosticsParserImpl : DiagnosticsParser {
  override fun parse(
    bazelOutputLines: List<String>,
    target: Label,
    isCommandLineFormattedOutput: Boolean,
    onlyFromParsedOutput: Boolean,
  ): List<Diagnostic> {
    val output = prepareOutput(bazelOutputLines, target)
    val diagnostics = collectDiagnostics(output, isCommandLineFormattedOutput, onlyFromParsedOutput)
    return deduplicate(diagnostics)
  }

  private fun prepareOutput(lines: List<String>, target: Label): Output {
    val relevantLines = lines.filterNot { it.isEmpty() }
    return Output(relevantLines, target)
  }

  private fun collectDiagnostics(output: Output, isCommandLineFormattedOutput: Boolean, onlyFromParsedOutput: Boolean): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()
    while (output.nonEmpty()) {
      val parsers = if (isCommandLineFormattedOutput) CommandLineOutputParser else Parsers
      for (parser in parsers) {
        val result = parser.tryParse(output)
        if (result.isNotEmpty()) {
          diagnostics.addAll(result)
          break
        }
      }
    }

    if (diagnostics.isEmpty() && !onlyFromParsedOutput) {
      diagnostics.add(
        Diagnostic(
          position = Position.NONE,
          message = output.fullOutput(),
          fileLocation = null,
          targetLabel = output.targetLabel,
        )
      )
    }

    return diagnostics.toList()
  }

  private fun deduplicate(parsedDiagnostics: List<Diagnostic>): List<Diagnostic> =
    parsedDiagnostics
      .groupBy { Triple(it.fileLocation, it.message, it.position) }
      .values
      .map { it.first() }
  companion object {
    private val Parsers =
      listOf(
        CompilerDiagnosticParser,
        IntelliJRunnerOutputParser,
        Scala3CompilerDiagnosticParser,
        AllCatchParser,
      )
    private val CommandLineOutputParser =
      listOf(
        BazelOutputMessageParser,
      )
  }
}
