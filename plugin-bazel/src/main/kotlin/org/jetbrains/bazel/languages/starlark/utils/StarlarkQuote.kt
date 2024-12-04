package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.openapi.util.TextRange

enum class StarlarkQuote(val quote: String) {
  SINGLE("'"),
  DOUBLE("\""),
  TRIPLE_SINGLE("'''"),
  TRIPLE_DOUBLE("\"\"\""),
  UNQUOTED(""),
  ;

  fun rangeWithinQuotes(string: String): TextRange = TextRange(quote.length, string.length - quote.length)

  fun wrap(toWrap: String): String = quote + toWrap + quote

  fun unwrap(toUnwrap: String): String = toUnwrap.removeSurrounding(quote)

  companion object {
    fun ofString(string: String): StarlarkQuote =
      when {
        string.startsWith(SINGLE.quote) -> SINGLE
        string.startsWith(DOUBLE.quote) -> DOUBLE
        string.startsWith(TRIPLE_SINGLE.quote) -> TRIPLE_SINGLE
        string.startsWith(TRIPLE_DOUBLE.quote) -> TRIPLE_DOUBLE
        else -> UNQUOTED
      }
  }
}
