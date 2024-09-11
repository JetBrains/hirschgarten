package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder.Marker
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

class FunctionParsing(context: ParsingContext) : Parsing(context) {
  fun parseFunction(functionMarker: Marker) {
    assertCurrentToken(StarlarkTokenTypes.DEF_KEYWORD)
    builder.advanceLexer()
    parseIdentifierOrSkip()
    parseParameterList()
    checkMatches(StarlarkTokenTypes.COLON, StarlarkBundle.message("parser.expected.colon"))
    context.pushScope(context.getScope().withFunction())
    context.statementParser.parseSuite(functionMarker, StarlarkElementTypes.FUNCTION_DECLARATION)
    context.popScope()
  }

  private fun parseParameterList() {
    if (!atToken(StarlarkTokenTypes.LPAR)) {
      builder.error(StarlarkBundle.message("parser.expected.lpar"))
      builder.mark().done(StarlarkElementTypes.PARAMETER_LIST) // To have non-empty parameters list at all the time.
      return
    }
    parseParameterListContents(StarlarkTokenTypes.RPAR, true)
  }

  fun parseParameterListContents(endToken: IElementType, advanceLexer: Boolean) {
    val parameterList = builder.mark()
    if (advanceLexer) {
      builder.advanceLexer()
    }
    var first = true
    var afterStarParameter = false
    while (!atToken(endToken)) {
      if (first) {
        first = false
      } else {
        if (atToken(StarlarkTokenTypes.COMMA)) {
          builder.advanceLexer()
        } else {
          builder.error(StarlarkBundle.message("parser.expected.comma.lpar.rpar"))
          break
        }
      }
      val isStarParameter = atAnyOfTokens(listOf(StarlarkTokenTypes.MULT, StarlarkTokenTypes.EXP))
      if (!parseParameter(endToken)) {
        if (afterStarParameter) {
          builder.error(StarlarkBundle.message("parser.expected.expression"))
        }
        break
      }
      if (isStarParameter) {
        afterStarParameter = true
      }
    }
    if (atToken(endToken) && endToken === StarlarkTokenTypes.RPAR) {
      builder.advanceLexer()
    }
    parameterList.done(StarlarkElementTypes.PARAMETER_LIST)
    if (atToken(endToken) && endToken === StarlarkTokenTypes.COLON) {
      builder.advanceLexer()
    }
  }

  private fun parseParameter(endToken: IElementType): Boolean {
    val parameter = builder.mark()
    var parameterType = StarlarkElementTypes.MANDATORY_PARAMETER
    if (atToken(StarlarkTokenTypes.MULT)) {
      builder.advanceLexer()
      parameterType = StarlarkElementTypes.VARIADIC_PARAMETER
    } else if (atToken(StarlarkTokenTypes.EXP)) {
      builder.advanceLexer()
      parameterType = StarlarkElementTypes.KEYWORD_VARIADIC_PARAMETER
    }
    if (matchToken(StarlarkTokenTypes.IDENTIFIER)) {
      if (parameterType == StarlarkElementTypes.MANDATORY_PARAMETER && matchToken(StarlarkTokenTypes.EQ)) {
        if (!context.expressionParser.parseSingleExpression(isTarget = false)) {
          val invalidElements = builder.mark()
          while (!atAnyOfTokens(listOf(endToken, StarlarkTokenTypes.LINE_BREAK, StarlarkTokenTypes.COMMA, null))) {
            nextToken()
          }
          invalidElements.error(StarlarkBundle.message("parser.expected.expression"))
        }
        parameter.done(StarlarkElementTypes.OPTIONAL_PARAMETER)
        return true
      }
      parameter.done(parameterType)
      return true
    } else {
      parameter.rollbackTo()
      if (atToken(endToken)) {
        return false
      }
      val invalidElements = builder.mark()
      while (!atAnyOfTokens(listOf(endToken, StarlarkTokenTypes.LINE_BREAK, StarlarkTokenTypes.COMMA, null))) {
        nextToken()
      }
      invalidElements.error(StarlarkBundle.message("parser.expected.formal.param.name"))
      return atToken(endToken) || atToken(StarlarkTokenTypes.COMMA)
    }
  }
}
