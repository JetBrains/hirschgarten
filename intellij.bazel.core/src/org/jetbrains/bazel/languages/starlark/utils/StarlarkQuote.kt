package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.openapi.util.TextRange
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class StarlarkQuote(val quote: String) {
  SINGLE("'"),
  DOUBLE("\""),
  TRIPLE_SINGLE("'''"),
  TRIPLE_DOUBLE("\"\"\""),
  UNQUOTED(""),
  ;

  fun rangeWithinQuotes(string: String): TextRange {
    val hasClosingQuote = string.length >= quote.length * 2 &&
      string.endsWith(quote) &&
      string.dropLast(quote.length).takeLastWhile { it == '\\' }.length % 2 == 0
    val endOffset = if (hasClosingQuote) string.length - quote.length else string.length
    return TextRange(quote.length, endOffset)
  }

  fun wrap(toWrap: String): String = quote + toWrap + quote

  fun unwrap(toUnwrap: String): String = toUnwrap.removeSurrounding(quote)

  companion object {
    fun ofString(string: String): StarlarkQuote =
      when {
        string.startsWith(TRIPLE_SINGLE.quote) -> TRIPLE_SINGLE
        string.startsWith(TRIPLE_DOUBLE.quote) -> TRIPLE_DOUBLE
        string.startsWith(SINGLE.quote) -> SINGLE
        string.startsWith(DOUBLE.quote) -> DOUBLE
        else -> UNQUOTED
      }
  }
}
