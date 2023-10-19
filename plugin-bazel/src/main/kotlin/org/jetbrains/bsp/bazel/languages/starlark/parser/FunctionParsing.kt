package org.jetbrains.bsp.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder.Marker
import com.intellij.psi.tree.IElementType
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bsp.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bsp.bazel.languages.starlark.elements.StarlarkTokenTypes

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
      if (atToken(StarlarkTokenTypes.LPAR)) {
        parseParameterSubList()
        continue
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
    var isStarParameter = false
    if (atToken(StarlarkTokenTypes.MULT)) {
      builder.advanceLexer()
      if (atToken(StarlarkTokenTypes.COMMA) || atToken(endToken)) {
        parameter.done(StarlarkElementTypes.SINGLE_STAR_PARAMETER)
        return true
      }
      isStarParameter = true
    } else if (atToken(StarlarkTokenTypes.EXP)) {
      builder.advanceLexer()
      isStarParameter = true
    }
    if (matchToken(StarlarkTokenTypes.IDENTIFIER)) {
      if (!isStarParameter && matchToken(StarlarkTokenTypes.EQ)) {
        if (!context.expressionParser.parseSingleExpression(isTarget = false)) {
          val invalidElements = builder.mark()
          while (!atAnyOfTokens(listOf(endToken, StarlarkTokenTypes.LINE_BREAK, StarlarkTokenTypes.COMMA, null))) {
            nextToken()
          }
          invalidElements.error(StarlarkBundle.message("parser.expected.expression"))
        }
      }
      parameter.done(StarlarkElementTypes.NAMED_PARAMETER)
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
    return true
  }

  private fun parseParameterSubList() {
    assertCurrentToken(StarlarkTokenTypes.LPAR)
    val tuple = builder.mark()
    builder.advanceLexer()
    while (true) {
      if (atToken(StarlarkTokenTypes.IDENTIFIER)) {
        val parameter = builder.mark()
        builder.advanceLexer()
        parameter.done(StarlarkElementTypes.NAMED_PARAMETER)
      } else if (atToken(StarlarkTokenTypes.LPAR)) {
        parseParameterSubList()
      }
      if (atToken(StarlarkTokenTypes.RPAR)) {
        builder.advanceLexer()
        break
      }
      if (!atToken(StarlarkTokenTypes.COMMA)) {
        builder.error(StarlarkBundle.message("parser.expected.comma.lpar.rpar"))
        break
      }
      builder.advanceLexer()
    }
    if (atToken(StarlarkTokenTypes.EQ)) {
      builder.advanceLexer()
      context.expressionParser.parseSingleExpression(isTarget = false)
    }
    tuple.done(StarlarkElementTypes.TUPLE_PARAMETER)
  }
}
