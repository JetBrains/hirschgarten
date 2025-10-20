package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import kotlin.io.path.exists

object BazelOutputMessageParser : Parser {
  private val COLOR = "\\u001B\\[[0-9]+m".toRegex()

  override fun tryParse(output: Output): List<Diagnostic> = findErrorInOutput(output)

  private val ErrorInOutput =
    """
      ^               # start of line
      ERROR:\s*       # error indicator
      (.*)            # error message
      $               # end of line
      """.toRegex(RegexOption.COMMENTS)

  private val FileBasedError =
    """
      ^                           # start of line
      ?(?<file>[^:\r\n]+)         # file path
      :(?<line>\d+)               # line number
      :(?<char>\d+)               # caret position
      :\s*(?<message>.+?)\s*$     # error message
      """.toRegex(RegexOption.COMMENTS)

  private fun findErrorInOutput(output: Output): List<Diagnostic> =
    generateSequence { output.tryTake() }
      .mapNotNull { line ->
        val strippedLine = line.replace(COLOR, "")
        ErrorInOutput.matchEntire(strippedLine)?.let { match ->
          createError(match, output.targetLabel)
        }
      }.toList()

  private fun createError(match: MatchResult, targetLabel: Label): Diagnostic {
    FileBasedError.matchEntire(match.groupValues[1])?.let { fileMatch ->
      val file =
        try {
          fileMatch.groups["file"]!!.let { Paths.get(it.value) }.takeIf { file -> file.exists() }
        } catch (_: InvalidPathException) {
          null
        }
      if (file != null) {
        val position =
          Position(
            fileMatch.groups["line"]!!.value.toInt(),
            fileMatch.groups["char"]!!.value.toInt(),
          )
        val message = fileMatch.groups["message"]!!.value
        return Diagnostic(position, message, file, targetLabel, DiagnosticSeverity.ERROR)
      }
    }
    return Diagnostic(
      position = Position(-1, -1),
      message = match.groupValues[1],
      fileLocation = null,
      targetLabel = targetLabel,
      level = DiagnosticSeverity.ERROR,
    )
  }
}
