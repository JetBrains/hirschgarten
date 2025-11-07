package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bsp.protocol.DiagnosticSeverity
import kotlin.io.path.Path

object IntelliJRunnerOutputParser : Parser {

  private val HeadlineRegex = Regex(
    """
      ^
      \s*
      (?<runner>[A-Za-z0-9\s._-]+[Rr][Uu][Nn][Nn][Ee][Rr]): # headline must contain 'runner' with any casing
      \s*
      (?<severity>\S+):
      \s*
      (?<message>.*)
      $
      """.trimIndent(),
    option = RegexOption.COMMENTS,
  )
  private val LocationLineRegex = Regex(
    """
      ^
      \s*
      (?<path>\S(?:.*\S)?)
      \s*
      \((?<line>\d+):(?<column>\d+)\)
      \s*
      $
      """.trimIndent(),
    option = RegexOption.COMMENTS,
  )


  override fun tryParse(output: Output): List<Diagnostic> {
    val peeked = output.peekAll()
    val headlineText = peeked.firstOrNull() ?: return emptyList()
    val headlineMatch = HeadlineRegex.matchEntire(headlineText) ?: return emptyList()
    val severity = when (headlineMatch.groupValues[2]) {
      "Error" -> DiagnosticSeverity.ERROR
      "Warning" -> DiagnosticSeverity.WARNING
      else -> return emptyList()
    }
    val message = StringBuilder(headlineMatch.groupValues[3])
    for (i in 1..peeked.lastIndex) {
      val textLine = peeked[i]
      val locationMatch = LocationLineRegex.matchEntire(textLine)
      if (locationMatch == null) {
        message.appendLine()
        message.append(textLine)
        continue
      }
      val (path, lineText, columnText) = locationMatch.destructured
      val position = Position(lineText.toInt(), columnText.toInt())
      output.take(i + 1)
      return listOf(
        Diagnostic(
          position = position,
          message = message.toString(),
          fileLocation = Path(path),
          targetLabel = output.targetLabel,
          level = severity,
        ),
      )
    }
    return emptyList()
  }
}

