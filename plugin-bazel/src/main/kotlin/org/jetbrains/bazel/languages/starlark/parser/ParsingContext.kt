package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder
import java.util.*

class ParsingContext(val builder: PsiBuilder) {
  val expressionParser = ExpressionParsing(this)
  val statementParser = StatementParsing(this)
  val functionParser = FunctionParsing(this)
  private val scopes = ArrayDeque(listOf(emptyParsingScope()))

  fun popScope(): ParsingScope {
    val prevScope = scopes.pop()
    resetBuilderCache(prevScope)
    return prevScope
  }

  fun pushScope(scope: ParsingScope) {
    val prevScope = getScope()
    scopes.push(scope)
    resetBuilderCache(prevScope)
  }

  fun getScope(): ParsingScope = scopes.peek()

  fun emptyParsingScope(): ParsingScope = ParsingScope()

  private fun resetBuilderCache(prevScope: ParsingScope) {
    if (getScope() != prevScope) {
      builder.mark().rollbackTo()
    }
  }
}
