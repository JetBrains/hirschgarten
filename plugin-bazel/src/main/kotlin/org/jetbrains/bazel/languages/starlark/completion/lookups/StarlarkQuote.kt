package org.jetbrains.bazel.languages.starlark.completion.lookups

enum class StarlarkQuote(val quote: String) {
  DOUBLE("\""),
  TRIPLE("\"\"\""),
  UNQUOTED(""),
  ;

  fun wrap(toWrap: String): String = quote + toWrap + quote
}
