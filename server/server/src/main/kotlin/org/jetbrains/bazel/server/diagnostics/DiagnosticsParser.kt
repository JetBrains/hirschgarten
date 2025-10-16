package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label

interface DiagnosticsParser {
  fun parse(
    bazelOutput: String,
    target: Label,
    isCommandLineFormattedOutput: Boolean = false,
  ): List<Diagnostic>
}

class DiagnosticsParserImpl : DiagnosticsParser {
  override fun parse(
    bazelOutput: String,
    target: Label,
    isCommandLineFormattedOutput: Boolean,
  ): List<Diagnostic> {
    val output = prepareOutput(bazelOutput, target)
    val diagnostics = collectDiagnostics(output, isCommandLineFormattedOutput)
    return deduplicate(diagnostics)
  }

  private fun prepareOutput(bazelOutput: String, target: Label): Output {
    val lines = bazelOutput.lines()
    val relevantLines = lines.filterNot { line -> IgnoredLines.any { it.matches(line) } }
    return Output(relevantLines, target)
  }

  private fun collectDiagnostics(output: Output, isCommandLineFormattedOutput: Boolean): List<Diagnostic> {
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

    if (diagnostics.isEmpty() && output.containsError()) {
      diagnostics.add(
        Diagnostic(
          position = Position(0, 0),
          message = output.fullOutput(),
          fileLocation = null,
          targetLabel = output.targetLabel,
        ),

      )
    }

    return diagnostics.toList()
  }

  private fun deduplicate(parsedDiagnostics: List<Diagnostic>): List<Diagnostic> =
    parsedDiagnostics
      .groupBy { Triple(it.fileLocation, it.message, it.position) }
      .values
      .map { it.first() }

  private fun Output.containsError(): Boolean = lines.any { line -> ErrorMessages.any { errorMessage -> errorMessage in line } }

  companion object {
    private val Parsers =
      listOf(
        BazelRootMessageParser,
        CompilerDiagnosticParser,
        Scala3CompilerDiagnosticParser,
        AllCatchParser,
      )
    private val CommandLineOutputParser =
      listOf(
        BazelOutputMessageParser,
      )
    private val IgnoredLines =
      listOf(
        "^$".toRegex(),
        "Use --sandbox_debug to see verbose messages from the sandbox".toRegex(),
      )
    private val ErrorMessages = listOf("error", "Error", "ERROR")
  }
}
