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

  private val FileInOutput =
    """
      ^\s*                        # start of line
      File\s*"(?<file>[^"]*)",   # file path
      \s*line\s*(?<line>\d+),     # line number
      \s*column\s*(?<char>\d+)    # caret position
      .*$                         # the rest
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
          val lines = listOf (match.groups[1]!!.value) + generateSequence {
            output.peek()
              ?.replace(COLOR, "")
              ?.takeUnless { ErrorInOutput.matchEntire(it) != null }
              ?.also { output.take() }
          }.filter { it.isNotBlank() }
          createError(lines, output.targetLabel)
        }
      }.toList()

  private fun createError(lines: List<String>, targetLabel: Label): Diagnostic {
    var fileMatch : MatchResult? = null
    val message = mutableListOf<String>()
    lines.forEach { line ->
      FileBasedError.matchEntire(line)?.let { match ->
        fileMatch = match
        message.add(match.groups["message"]!!.value)
      } ?: message.add(line).also {
        FileInOutput.matchEntire(line)?.let { match ->
          fileMatch = match
        }
      }
    }
    fileMatch?.let { fileMatch ->
      val file = try {
        fileMatch.groups["file"]!!.let { Paths.get(it.value) }.takeIf { file -> file.exists() }
      } catch (_: InvalidPathException) {
        null
      }
      if (file != null) {
        val position = Position(
          fileMatch.groups["line"]!!.value.toInt(),
          fileMatch.groups["char"]!!.value.toInt(),
        )
        val message = message.joinToString("\n").trim()
        return Diagnostic(position, message, file, targetLabel, DiagnosticSeverity.ERROR)
      }
    }
    return Diagnostic(
      position = Position(-1, -1),
      message = message.joinToString("\n").trim(),
      fileLocation = null,
      targetLabel = targetLabel,
      level = DiagnosticSeverity.ERROR,
    )
  }
}
