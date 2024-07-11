package org.jetbrains.bazel.languages.starlark.parser

data class ParsingScope(
  val isFunction: Boolean = false, val isSuite: Boolean = false, var isAfterSemicolon: Boolean = false
) {
  fun withFunction(): ParsingScope = copy(isFunction = false)

  fun withSuite(): ParsingScope = copy(isSuite = true)
}
