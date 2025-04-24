package org.jetbrains.bazel.ui.widgets.tool.window.components

object QueryHighlighter {
  fun highlight(text: String, query: Regex): String {
    val matches = query.findAll(text).take(text.length).toList()
    return if (query.pattern.isNotEmpty() && matches.isNotEmpty()) {
      "<html>${highlightAllOccurrences(text, matches)}</html>"
    } else {
      text
    }
  }

  private fun highlightAllOccurrences(text: String, query: List<MatchResult>): String {
    var lastIndex = 0
    val result =
      buildString {
        for (match in query) {
          val matchStart = match.range.first
          val matchEnd = match.range.last + 1
          append(text, lastIndex, matchStart)
          append("<b><u>")
          append(text, matchStart, matchEnd)
          append("</u></b>")
          lastIndex = matchEnd
        }
        append(text.substring(lastIndex))
      }
    return result
  }
}
