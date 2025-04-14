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
    val result = StringBuilder()
    var lastIndex = 0
    for (match in query) {
      val matchStart = match.range.first
      val matchEnd = match.range.last + 1
      result.append(text, lastIndex, matchStart)
      result.append("<b><u>")
      result.append(text, matchStart, matchEnd)
      result.append("</u></b>")
      lastIndex = matchEnd
    }
    result.append(text.substring(lastIndex))
    return result.toString()
  }
}
