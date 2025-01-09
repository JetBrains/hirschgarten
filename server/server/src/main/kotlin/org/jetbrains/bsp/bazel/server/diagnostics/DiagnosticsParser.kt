package org.jetbrains.bsp.bazel.server.diagnostics

import org.jetbrains.bazel.commons.label.Label

interface DiagnosticsParser {
  fun parse(bazelOutput: String, target: Label, fromProgress: Boolean): List<Diagnostic>
}

class DiagnosticsParserImpl : DiagnosticsParser {
  override fun parse(bazelOutput: String, target: Label, fromProgress: Boolean): List<Diagnostic> {
    val output = prepareOutput(bazelOutput, target)
    val diagnostics = collectDiagnostics(output, fromProgress)
    return deduplicate(diagnostics)
  }

  private fun prepareOutput(bazelOutput: String, target: Label): Output {
    val lines = bazelOutput.lines()
    val relevantLines = lines.filterNot { line -> IgnoredLines.any { it.matches(line) } }
    return Output(relevantLines, target)
  }

  private fun collectDiagnostics(output: Output, fromProgress: Boolean): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()
    val parsers = if (fromProgress) FromProgressParsers else Parsers
    while (output.nonEmpty()) {
      for (parser in parsers) {
        val result = parser.tryParse(output)
        if (result.isNotEmpty()) {
          diagnostics.addAll(result)
          break
        }
      }
    }

    if (diagnostics.isEmpty()) {
      diagnostics.add(
        Diagnostic(
          position = Position(0, 0),
          message = output.fullOutput(),
          fileLocation = "<unknown>",
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

  companion object {
    // Include *only* these parsers which handle messages that cannot be obtained in any other way
    private val FromProgressParsers = listOf(
      // Handles syntax errors in build files
      BazelRootMessageParser,
      // Advances the output if nothing else can be parsed
      AllCatchParser,
    )
    private val Parsers =
      listOf(
        BazelRootMessageParser,
        CompilerDiagnosticParser,
        Scala3CompilerDiagnosticParser,
        AllCatchParser,
      )
    private val IgnoredLines =
      listOf(
        "^$".toRegex(),
        "Use --sandbox_debug to see verbose messages from the sandbox".toRegex(),
      )
  }
}
