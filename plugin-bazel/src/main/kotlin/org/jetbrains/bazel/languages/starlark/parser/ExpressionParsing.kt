package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder.Marker
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

class ExpressionParsing(context: ParsingContext) : Parsing(context) {
  private fun parsePrimaryExpression(isTarget: Boolean): Boolean =
    if (builder.tokenType in StarlarkTokenSets.PRIMARY_EXPRESSION_STARTERS) {
      when (builder.tokenType) {
        StarlarkTokenTypes.IDENTIFIER -> buildTokenElement(getTargetOrReferenceExpression(isTarget), builder)
        StarlarkTokenTypes.INT -> buildTokenElement(StarlarkElementTypes.INTEGER_LITERAL_EXPRESSION, builder)
        StarlarkTokenTypes.FLOAT -> buildTokenElement(StarlarkElementTypes.FLOAT_LITERAL_EXPRESSION, builder)
        StarlarkTokenTypes.LPAR -> parseParenthesizedExpression(isTarget)
        StarlarkTokenTypes.LBRACKET -> parseListLiteralExpression(isTarget)
        StarlarkTokenTypes.LBRACE -> parseDictDisplay()
        else -> buildTokenElement(StarlarkElementTypes.STRING_LITERAL_EXPRESSION, builder)
      }
      true
    } else {
      false
    }

  private fun parseListLiteralExpression(isTarget: Boolean) {
    assertCurrentToken(StarlarkTokenTypes.LBRACKET)
    val expr = builder.mark()
    builder.advanceLexer()
    if (atToken(StarlarkTokenTypes.RBRACKET)) {
      builder.advanceLexer()
      expr.done(StarlarkElementTypes.LIST_LITERAL_EXPRESSION)
      return
    }
    if (!parseSingleExpression(isTarget)) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
    }
    if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
      parseComprehension(expr, StarlarkTokenTypes.RBRACKET, StarlarkElementTypes.LIST_COMP_EXPRESSION)
    } else {
      while (!atToken(StarlarkTokenTypes.RBRACKET)) {
        if (!matchToken(StarlarkTokenTypes.COMMA)) {
          builder.error(StarlarkBundle.message("parser.rbracket.or.comma.expected"))
        }
        if (atToken(StarlarkTokenTypes.RBRACKET)) {
          break
        }
        if (!parseSingleExpression(isTarget)) {
          builder.error(StarlarkBundle.message("parser.expected.expr.or.comma.or.bracket"))
          break
        }
      }
      checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkBundle.message("parser.expected.rbracket"))
      expr.done(StarlarkElementTypes.LIST_LITERAL_EXPRESSION)
    }
  }

  private fun parseComprehension(
    expr: Marker,
    endToken: IElementType?,
    exprType: IElementType,
  ) {
    assertCurrentToken(StarlarkTokenTypes.FOR_KEYWORD)
    while (true) {
      builder.advanceLexer()
      parseStarTargets()
      parseComprehensionRange(exprType === StarlarkElementTypes.GENERATOR_EXPRESSION)
      while (atToken(StarlarkTokenTypes.IF_KEYWORD)) {
        builder.advanceLexer()
        if (!parseOldTestExpression(oldTest = false)) {
          builder.error(StarlarkBundle.message("parser.expected.expression"))
        }
      }
      if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
        continue
      }
      if (endToken == null || matchToken(endToken)) {
        break
      }
      builder.error(StarlarkBundle.message("parser.expected.for.or.bracket"))
      break
    }
    expr.done(exprType)
  }

  fun parseStarTargets() {
    val expr = builder.mark()
    if (!parseStarExpression(true)) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
      expr.drop()
      return
    }
    if (atToken(StarlarkTokenTypes.COMMA)) {
      while (atToken(StarlarkTokenTypes.COMMA)) {
        builder.advanceLexer()
        val expr2 = builder.mark()
        if (!parseStarExpression(true)) {
          builder.error(StarlarkBundle.message("parser.expected.expression"))
          expr2.rollbackTo()
          break
        }
        expr2.drop()
      }
      expr.done(StarlarkElementTypes.TUPLE_EXPRESSION)
    } else {
      expr.drop()
    }
  }

  private fun parseComprehensionRange(generatorExpression: Boolean) {
    checkMatches(StarlarkTokenTypes.IN_KEYWORD, StarlarkBundle.message("parser.expected.in"))
    val result =
      if (generatorExpression) {
        parseORTestExpression(isTarget = false)
      } else {
        parseTupleExpression(isTarget = false, oldTest = true)
      }
    if (!result) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
    }
  }

  private fun parseDictDisplay() {
    assertCurrentToken(StarlarkTokenTypes.LBRACE)
    val expr = builder.mark()
    builder.advanceLexer()
    if (matchToken(StarlarkTokenTypes.RBRACE)) {
      expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION)
      return
    }
    if (atToken(StarlarkTokenTypes.EXP)) {
      if (!parseDoubleStarExpression()) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
        expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION)
        return
      }
      parseDictLiteralContentTail(expr)
      return
    }
    parseDictTail(expr, isDict = false)
  }

  private fun parseDictTail(expr: Marker, isDict: Boolean) {
    val firstExprMarker = builder.mark()
    if (isDict && !parseSingleExpression(isTarget = false) || !isDict && !parseSingleExpression(isTarget = false)) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
      firstExprMarker.drop()
      expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION)
      return
    }
    if (matchToken(StarlarkTokenTypes.COLON)) {
      if (!isDict) {
        firstExprMarker.rollbackTo()
        parseDictTail(expr, true)
        return
      }
      parseDictLiteralTail(expr, firstExprMarker)
    } else {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
      firstExprMarker.drop()
      expr.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION)
    }
  }

  private fun parseDictLiteralTail(startMarker: Marker, firstKeyValueMarker: Marker) {
    if (!parseSingleExpression(isTarget = false)) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
      firstKeyValueMarker.done(StarlarkElementTypes.KEY_VALUE_EXPRESSION)
      if (atToken(StarlarkTokenTypes.RBRACE)) {
        builder.advanceLexer()
      }
      startMarker.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION)
      return
    }
    firstKeyValueMarker.done(StarlarkElementTypes.KEY_VALUE_EXPRESSION)
    if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
      parseComprehension(startMarker, StarlarkTokenTypes.RBRACE, StarlarkElementTypes.DICT_COMP_EXPRESSION)
    } else {
      parseDictLiteralContentTail(startMarker)
    }
  }

  private fun parseDictLiteralContentTail(startMarker: Marker) {
    while (!atToken(StarlarkTokenTypes.RBRACE)) {
      checkMatches(StarlarkTokenTypes.COMMA, StarlarkBundle.message("parser.expected.comma"))
      if (atToken(StarlarkTokenTypes.EXP)) {
        if (!parseDoubleStarExpression()) {
          break
        }
      } else {
        if (!parseKeyValueExpression()) {
          break
        }
      }
    }
    checkMatches(StarlarkTokenTypes.RBRACE, StarlarkBundle.message("parser.expected.rbrace"))
    startMarker.done(StarlarkElementTypes.DICT_LITERAL_EXPRESSION)
  }

  private fun parseKeyValueExpression(): Boolean {
    val marker = builder.mark()
    if (!parseSingleExpression(isTarget = false)) {
      marker.drop()
      return false
    }
    checkMatches(StarlarkTokenTypes.COLON, StarlarkBundle.message("parser.expected.colon"))
    if (!parseSingleExpression(isTarget = false)) {
      builder.error(StarlarkBundle.message("parser.value.expression.expected"))
      marker.drop()
      return false
    }
    marker.done(StarlarkElementTypes.KEY_VALUE_EXPRESSION)
    return true
  }

  private fun parseParenthesizedExpression(isTarget: Boolean) {
    assertCurrentToken(StarlarkTokenTypes.LPAR)
    val expr = builder.mark()
    builder.advanceLexer()
    if (atToken(StarlarkTokenTypes.RPAR)) {
      builder.advanceLexer()
      expr.done(StarlarkElementTypes.TUPLE_EXPRESSION)
    } else {
      parseTupleExpression(isTarget, oldTest = false)
      if (atToken(StarlarkTokenTypes.FOR_KEYWORD)) {
        parseComprehension(expr, StarlarkTokenTypes.RPAR, StarlarkElementTypes.GENERATOR_EXPRESSION)
      } else {
        val err = builder.mark()
        var empty = true
        while (!builder.eof() &&
          !atAnyOfTokens(
            listOf(StarlarkTokenTypes.RPAR, StarlarkTokenTypes.LINE_BREAK, StarlarkTokenTypes.STATEMENT_BREAK),
          )
        ) {
          builder.advanceLexer()
          empty = false
        }
        if (!empty) {
          err.error(StarlarkBundle.message("parser.unexpected.expression.syntax"))
        } else {
          err.drop()
        }
        checkMatches(StarlarkTokenTypes.RPAR, StarlarkBundle.message("parser.expected.rpar"))
        expr.done(StarlarkElementTypes.PARENTHESIZED_EXPRESSION)
      }
    }
  }

  private fun parseMemberExpression(isTarget: Boolean): Boolean {
    // in sequence a.b.... .c all members but last are always references, and the last may be target.
    var recastFirstIdentifier = false
    var recastQualifier = false
    do {
      val firstIdentifierIsTarget = isTarget && !recastFirstIdentifier
      var expr = builder.mark()
      if (!parsePrimaryExpression(firstIdentifierIsTarget)) {
        expr.drop()
        return false
      }
      while (true) {
        val tokenType = builder.tokenType
        if (tokenType === StarlarkTokenTypes.DOT) {
          if (firstIdentifierIsTarget) {
            recastFirstIdentifier = true
            expr.rollbackTo()
            break
          }
          builder.advanceLexer()
          checkMatches(StarlarkTokenTypes.IDENTIFIER, StarlarkBundle.message("parser.expected.name"))
          if (isTarget &&
            !recastQualifier &&
            !atAnyOfTokens(
              listOf(StarlarkTokenTypes.DOT, StarlarkTokenTypes.LPAR, StarlarkTokenTypes.LBRACKET),
            )
          ) {
            expr.done(StarlarkElementTypes.TARGET_EXPRESSION)
          } else {
            expr.done(StarlarkElementTypes.REFERENCE_EXPRESSION)
          }
          expr = expr.precede()
        } else if (tokenType === StarlarkTokenTypes.LPAR) {
          parseArgumentList()
          expr.done(StarlarkElementTypes.CALL_EXPRESSION)
          expr = expr.precede()
        } else if (tokenType === StarlarkTokenTypes.LBRACKET) {
          builder.advanceLexer()
          parseSliceOrSubscriptionExpression(expr, isSlice = false)
          if (isTarget && !recastQualifier) {
            recastFirstIdentifier = true // subscription is always a reference
            recastQualifier = true // recast non-first qualifiers too
            expr.rollbackTo()
            break
          }
          expr = expr.precede()
        } else {
          expr.drop()
          break
        }
        recastFirstIdentifier = false // it is true only after a break; normal flow always unsets it.
        // recastQualifier is untouched, it remembers whether qualifiers were already recast
      }
    } while (recastFirstIdentifier)
    return true
  }

  private fun parseSliceOrSubscriptionExpression(expr: Marker, isSlice: Boolean) {
    val sliceOrTupleStart = builder.mark()
    val sliceItemStart = builder.mark()
    if (atToken(StarlarkTokenTypes.COLON)) {
      sliceOrTupleStart.drop()
      val sliceMarker = builder.mark()
      sliceMarker.done(StarlarkElementTypes.EMPTY_EXPRESSION)
      parseSliceEnd(expr, sliceItemStart)
    } else {
      val hadExpression = parseSingleExpression(isTarget = false)
      if (atToken(StarlarkTokenTypes.COLON)) {
        if (!isSlice) {
          sliceOrTupleStart.rollbackTo()
          parseSliceOrSubscriptionExpression(expr, isSlice = true)
          return
        }
        sliceOrTupleStart.drop()
        parseSliceEnd(expr, sliceItemStart)
      } else if (atToken(StarlarkTokenTypes.COMMA)) {
        sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM)
        if (!parseSliceListTail(expr, sliceOrTupleStart)) {
          sliceOrTupleStart.rollbackTo()
          if (!parseTupleExpression(isTarget = false, oldTest = false)) {
            builder.error(StarlarkBundle.message("parser.tuple.expression.expected"))
          }
          checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkBundle.message("parser.expected.rbracket"))
          expr.done(StarlarkElementTypes.SUBSCRIPTION_EXPRESSION)
        }
      } else {
        if (!hadExpression) {
          builder.error(StarlarkBundle.message("parser.expected.expression"))
        }
        sliceOrTupleStart.drop()
        sliceItemStart.drop()
        checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkBundle.message("parser.expected.rbracket"))
        expr.done(StarlarkElementTypes.SUBSCRIPTION_EXPRESSION)
      }
    }
  }

  private fun parseSliceEnd(exprStart: Marker, sliceItemStart: Marker) {
    builder.advanceLexer()
    if (atToken(StarlarkTokenTypes.RBRACKET)) {
      val sliceMarker = builder.mark()
      sliceMarker.done(StarlarkElementTypes.EMPTY_EXPRESSION)
      sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM)
      nextToken()
      exprStart.done(StarlarkElementTypes.SLICE_EXPRESSION)
      return
    } else {
      if (atToken(StarlarkTokenTypes.COLON)) {
        val sliceMarker = builder.mark()
        sliceMarker.done(StarlarkElementTypes.EMPTY_EXPRESSION)
      } else {
        parseSingleExpression(isTarget = false)
      }
      if (!BRACKET_COLON_COMMA.contains(builder.tokenType)) {
        builder.error(StarlarkBundle.message("parser.expected.colon.or.rbracket"))
      }
      if (matchToken(StarlarkTokenTypes.COLON)) {
        parseSingleExpression(isTarget = false)
      }
      sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM)
      if (!BRACKET_OR_COMMA.contains(builder.tokenType)) {
        builder.error(StarlarkBundle.message("parser.rbracket.or.comma.expected"))
      }
    }
    parseSliceListTail(exprStart, null)
  }

  private fun parseSliceListTail(exprStart: Marker, sliceOrTupleStart: Marker?): Boolean {
    var inSlice = sliceOrTupleStart == null
    while (atToken(StarlarkTokenTypes.COMMA)) {
      nextToken()
      val sliceItemStart = builder.mark()
      parseTestExpression(isTarget = false)
      if (matchToken(StarlarkTokenTypes.COLON)) {
        inSlice = true
        parseTestExpression(isTarget = false)
        if (matchToken(StarlarkTokenTypes.COLON)) {
          parseTestExpression(isTarget = false)
        }
      }
      sliceItemStart.done(StarlarkElementTypes.SLICE_ITEM)
      if (!BRACKET_OR_COMMA.contains(builder.tokenType)) {
        builder.error(StarlarkBundle.message("parser.rbracket.or.comma.expected"))
        break
      }
    }
    checkMatches(StarlarkTokenTypes.RBRACKET, StarlarkBundle.message("parser.expected.rbracket"))
    if (inSlice) {
      sliceOrTupleStart?.drop()
      exprStart.done(StarlarkElementTypes.SLICE_EXPRESSION)
    }
    return inSlice
  }

  private fun parseArgumentList() {
    assertCurrentToken(StarlarkTokenTypes.LPAR)
    val argumentList = builder.mark()
    builder.advanceLexer()
    var argumentCount = 0
    while (!atToken(StarlarkTokenTypes.RPAR)) {
      argumentCount++
      if (argumentCount > 1) {
        if (matchToken(StarlarkTokenTypes.COMMA)) {
          if (atToken(StarlarkTokenTypes.RPAR)) {
            break
          }
        } else {
          builder.error(StarlarkBundle.message("parser.expected.comma.or.rpar"))
          break
        }
      }
      if (atToken(StarlarkTokenTypes.MULT) || atToken(StarlarkTokenTypes.EXP)) {
        val starArgumentMarker = builder.mark()
        builder.advanceLexer()
        if (!parseSingleExpression(isTarget = false)) {
          builder.error(StarlarkBundle.message("parser.expected.expression"))
        }
        starArgumentMarker.done(StarlarkElementTypes.STAR_ARGUMENT_EXPRESSION)
      } else {
        if (isIdentifierLike(builder)) {
          val namedArgumentMarker = builder.mark()
          advanceIdentifierLike(builder)
          if (atToken(StarlarkTokenTypes.EQ)) {
            builder.advanceLexer()
            if (!parseSingleExpression(isTarget = false)) {
              builder.error(StarlarkBundle.message("parser.expected.expression"))
            }
            namedArgumentMarker.done(StarlarkElementTypes.NAMED_ARGUMENT_EXPRESSION)
            continue
          }
          namedArgumentMarker.rollbackTo()
        }
        val argumentMarker = builder.mark()
        if (!parseSingleExpression(isTarget = false)) {
          builder.error(StarlarkBundle.message("parser.expected.expression"))
          argumentMarker.rollbackTo()
          break
        }
        argumentMarker.done(StarlarkElementTypes.ARGUMENT_EXPRESSION)
      }
    }
    checkMatches(StarlarkTokenTypes.RPAR, StarlarkBundle.message("parser.expected.rpar"))
    argumentList.done(StarlarkElementTypes.ARGUMENT_LIST)
  }

  fun parseExpressionOptional(): Boolean = parseTupleExpression(isTarget = false, oldTest = false)

  fun parseExpression() {
    if (!parseExpressionOptional()) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
    }
  }

  fun parseExpression(isTarget: Boolean) {
    if (!parseTupleExpression(isTarget, oldTest = false)) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
    }
  }

  private fun parseTupleExpression(isTarget: Boolean, oldTest: Boolean): Boolean {
    val expr = builder.mark()
    var exprParseResult = if (oldTest) parseOldTestExpression(true) else parseTestExpression(isTarget)
    if (!exprParseResult) {
      expr.drop()
      return false
    }
    if (atToken(StarlarkTokenTypes.COMMA)) {
      while (atToken(StarlarkTokenTypes.COMMA)) {
        builder.advanceLexer()
        val expr2 = builder.mark()
        exprParseResult = if (oldTest) parseOldTestExpression(true) else parseTestExpression(isTarget)
        if (!exprParseResult) {
          expr2.rollbackTo()
          break
        }
        expr2.drop()
      }
      expr.done(StarlarkElementTypes.TUPLE_EXPRESSION)
    } else {
      expr.drop()
    }
    return true
  }

  fun parseSingleExpression(isTarget: Boolean): Boolean = parseTestExpression(isTarget)

  fun parseTestExpression(isTarget: Boolean): Boolean {
    if (atToken(StarlarkTokenTypes.LAMBDA_KEYWORD)) {
      return parseLambdaExpression(oldTest = false)
    }
    val condExpr = builder.mark()
    if (!parseORTestExpression(isTarget)) {
      condExpr.drop()
      return false
    }
    if (atToken(StarlarkTokenTypes.IF_KEYWORD)) {
      val conditionMarker = builder.mark()
      builder.advanceLexer()
      if (!parseORTestExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      } else {
        if (!atToken(StarlarkTokenTypes.ELSE_KEYWORD)) {
          if (atToken(StarlarkTokenTypes.COLON)) { // it's regular if statement. Bracket wasn't closed or new line was lost
            conditionMarker.rollbackTo()
            condExpr.drop()
            return true
          } else {
            builder.error(StarlarkBundle.message("parser.expected.else"))
          }
        } else {
          builder.advanceLexer()
          if (!parseTestExpression(isTarget)) {
            builder.error(StarlarkBundle.message("parser.expected.expression"))
          }
        }
      }
      conditionMarker.drop()
      condExpr.done(StarlarkElementTypes.CONDITIONAL_EXPRESSION)
    } else {
      condExpr.drop()
    }
    return true
  }

  private fun parseOldTestExpression(oldTest: Boolean): Boolean =
    if (atToken(StarlarkTokenTypes.LAMBDA_KEYWORD)) {
      parseLambdaExpression(oldTest)
    } else {
      parseORTestExpression(isTarget = false)
    }

  private fun parseLambdaExpression(oldTest: Boolean): Boolean {
    val expr = builder.mark()
    builder.advanceLexer()
    context.functionParser.parseParameterListContents(StarlarkTokenTypes.COLON, advanceLexer = false)
    val parseExpressionResult =
      if (oldTest) parseOldTestExpression(oldTest = true) else parseSingleExpression(isTarget = false)
    if (!parseExpressionResult) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
    }
    expr.done(StarlarkElementTypes.LAMBDA_EXPRESSION)
    return true
  }

  private fun parseORTestExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseANDTestExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (atToken(StarlarkTokenTypes.OR_KEYWORD)) {
      builder.advanceLexer()
      if (!parseANDTestExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseANDTestExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseComparisonExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (atToken(StarlarkTokenTypes.AND_KEYWORD)) {
      builder.advanceLexer()
      if (!parseComparisonExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseComparisonExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseStarExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (StarlarkTokenSets.COMPARISON_OPERATIONS.contains(builder.tokenType)) {
      if (atToken(StarlarkTokenTypes.NOT_KEYWORD)) {
        val notMarker = builder.mark()
        builder.advanceLexer()
        if (!atToken(StarlarkTokenTypes.IN_KEYWORD)) {
          notMarker.rollbackTo()
          break
        }
        notMarker.drop()
        builder.advanceLexer()
      } else if (atToken(StarlarkTokenTypes.IS_KEYWORD)) {
        builder.advanceLexer()
        if (atToken(StarlarkTokenTypes.NOT_KEYWORD)) {
          builder.advanceLexer()
        }
      } else {
        builder.advanceLexer()
      }
      if (!parseBitwiseORExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseStarExpression(isTarget: Boolean): Boolean {
    if (atToken(StarlarkTokenTypes.MULT)) {
      val starExpr = builder.mark()
      nextToken()
      if (!parseBitwiseORExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
        starExpr.drop()
        return false
      }
      starExpr.done(StarlarkElementTypes.STAR_EXPRESSION)
      return true
    }
    return parseBitwiseORExpression(isTarget)
  }

  private fun parseDoubleStarExpression(): Boolean {
    if (atToken(StarlarkTokenTypes.EXP)) {
      val starExpr = builder.mark()
      nextToken()
      if (!parseBitwiseORExpression(isTarget = false)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
        starExpr.drop()
        return false
      }
      starExpr.done(StarlarkElementTypes.DOUBLE_STAR_EXPRESSION)
      return true
    }
    return parseBitwiseORExpression(isTarget = false)
  }

  private fun parseBitwiseORExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseBitwiseXORExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (atToken(StarlarkTokenTypes.OR)) {
      builder.advanceLexer()
      if (!parseBitwiseXORExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseBitwiseXORExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseBitwiseANDExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (atToken(StarlarkTokenTypes.XOR)) {
      builder.advanceLexer()
      if (!parseBitwiseANDExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseBitwiseANDExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseShiftExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (atToken(StarlarkTokenTypes.AND)) {
      builder.advanceLexer()
      if (!parseShiftExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseShiftExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseAdditiveExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (StarlarkTokenSets.SHIFT_OPERATIONS.contains(builder.tokenType)) {
      builder.advanceLexer()
      if (!parseAdditiveExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseAdditiveExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseMultiplicativeExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (StarlarkTokenSets.ADDITIVE_OPERATIONS.contains(builder.tokenType)) {
      builder.advanceLexer()
      if (!parseMultiplicativeExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseMultiplicativeExpression(isTarget: Boolean): Boolean {
    var expr = builder.mark()
    if (!parseUnaryExpression(isTarget)) {
      expr.drop()
      return false
    }
    while (StarlarkTokenSets.MULTIPLICATIVE_OPERATIONS.contains(builder.tokenType)) {
      builder.advanceLexer()
      if (!parseUnaryExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
      expr = expr.precede()
    }
    expr.drop()
    return true
  }

  private fun parseUnaryExpression(isTarget: Boolean): Boolean =
    if (StarlarkTokenSets.UNARY_OPERATIONS.contains(builder.tokenType)) {
      val expr = builder.mark()
      builder.advanceLexer()
      if (!parseUnaryExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.PREFIX_EXPRESSION)
      true
    } else {
      parsePowerExpression(isTarget)
    }

  private fun parsePowerExpression(isTarget: Boolean): Boolean {
    val expr = builder.mark()
    if (!parseMemberExpression(isTarget)) {
      expr.drop()
      return false
    }
    if (atToken(StarlarkTokenTypes.EXP)) {
      builder.advanceLexer()
      if (!parseUnaryExpression(isTarget)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      expr.done(StarlarkElementTypes.BINARY_EXPRESSION)
    } else {
      expr.drop()
    }
    return true
  }

  private fun getTargetOrReferenceExpression(isTarget: Boolean) =
    if (isTarget) StarlarkElementTypes.TARGET_EXPRESSION else StarlarkElementTypes.REFERENCE_EXPRESSION

  companion object {
    private val BRACKET_OR_COMMA = TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.COMMA)
    private val BRACKET_COLON_COMMA =
      TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.COLON, StarlarkTokenTypes.COMMA)
  }
}
