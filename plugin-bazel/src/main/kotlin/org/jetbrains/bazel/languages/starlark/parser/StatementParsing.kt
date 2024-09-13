package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilder.Marker
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

class StatementParsing(context: ParsingContext) : Parsing(context) {
  fun parseStatement() {
    while (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
      builder.advanceLexer()
    }

    val firstToken = builder.tokenType ?: return
    when (firstToken) {
      StarlarkTokenTypes.IF_KEYWORD -> parseIfStatement()
      StarlarkTokenTypes.FOR_KEYWORD -> parseForStatement(builder.mark())
      StarlarkTokenTypes.DEF_KEYWORD -> context.functionParser.parseFunction(builder.mark())
      else -> parseSimpleStatement()
    }
  }

  fun parseSimpleStatement() {
    val firstToken = builder.tokenType ?: return
    when (firstToken) {
      StarlarkTokenTypes.BREAK_KEYWORD -> parseKeywordStatement(StarlarkElementTypes.BREAK_STATEMENT)
      StarlarkTokenTypes.CONTINUE_KEYWORD -> parseKeywordStatement(StarlarkElementTypes.CONTINUE_STATEMENT)
      StarlarkTokenTypes.PASS_KEYWORD -> parseKeywordStatement(StarlarkElementTypes.PASS_STATEMENT)
      StarlarkTokenTypes.RETURN_KEYWORD -> parseReturnStatement()
      StarlarkTokenTypes.LOAD_KEYWORD -> parseLoadStatement()
      else -> parseExpressionStatement(firstToken)
    }
  }

  private fun reportParseStatementError(builder: PsiBuilder, firstToken: IElementType) {
    if (firstToken === StarlarkTokenTypes.INCONSISTENT_DEDENT) {
      builder.error(StarlarkBundle.message("parser.unindent.does.not.match.any.outer.indent"))
    } else if (firstToken === StarlarkTokenTypes.INDENT) {
      builder.error(StarlarkBundle.message("parser.unexpected.indent"))
    } else {
      builder.error(StarlarkBundle.message("parser.statement.expected.found.0", firstToken.toString()))
    }
  }

  private fun checkEndOfStatement() {
    val scope = context.getScope()
    if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
      builder.advanceLexer()
      scope.isAfterSemicolon = false
    } else if (atToken(StarlarkTokenTypes.SEMICOLON)) {
      if (!scope.isSuite) {
        builder.advanceLexer()
        scope.isAfterSemicolon = true
        if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
          builder.advanceLexer()
          scope.isAfterSemicolon = false
        }
      }
    } else if (!builder.eof()) {
      builder.error(StarlarkBundle.message("parser.end.of.statement.expected"))
    }
  }

  private fun parseKeywordStatement(statementType: IElementType) {
    val statement = builder.mark()
    builder.advanceLexer()
    checkEndOfStatement()
    statement.done(statementType)
  }

  private fun parseReturnStatement() {
    assertCurrentToken(StarlarkTokenTypes.RETURN_KEYWORD)
    val returnStatement = builder.mark()
    builder.advanceLexer()
    if (builder.tokenType != null && !StarlarkTokenSets.ENDS_OF_STATEMENT.contains(builder.tokenType)) {
      context.expressionParser.parseExpression()
    }
    checkEndOfStatement()
    returnStatement.done(StarlarkElementTypes.RETURN_STATEMENT)
  }

  private fun parseIfStatement() {
    assertCurrentToken(StarlarkTokenTypes.IF_KEYWORD)
    val ifStatement = builder.mark()
    builder.advanceLexer()
    if (!context.expressionParser.parseTestExpression(isTarget = false)) {
      builder.error(StarlarkBundle.message("parser.expected.expression"))
    }
    parseColonAndSuite()
    while (atToken(StarlarkTokenTypes.ELIF_KEYWORD)) {
      builder.advanceLexer()
      if (!context.expressionParser.parseTestExpression(isTarget = false)) {
        builder.error(StarlarkBundle.message("parser.expected.expression"))
      }
      parseColonAndSuite()
    }
    if (atToken(StarlarkTokenTypes.ELSE_KEYWORD)) {
      builder.advanceLexer()
      parseColonAndSuite()
    }
    ifStatement.done(StarlarkElementTypes.IF_STATEMENT)
  }

  private fun expectColon(): Boolean {
    if (atToken(StarlarkTokenTypes.COLON)) {
      builder.advanceLexer()
      return true
    } else if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
      builder.error(StarlarkBundle.message("parser.expected.colon"))
      return true
    }
    val marker = builder.mark()
    while (!atAnyOfTokens(
        listOf(
          null,
          StarlarkTokenTypes.DEDENT,
          StarlarkTokenTypes.STATEMENT_BREAK,
          StarlarkTokenTypes.COLON,
        ),
      )
    ) {
      builder.advanceLexer()
    }
    val result = matchToken(StarlarkTokenTypes.COLON)
    if (!result && atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
      builder.advanceLexer()
    }
    marker.error(StarlarkBundle.message("parser.expected.colon"))
    return result
  }

  private fun parseForStatement(endMarker: Marker) {
    assertCurrentToken(StarlarkTokenTypes.FOR_KEYWORD)
    builder.advanceLexer()
    context.expressionParser.parseStarTargets()
    checkMatches(StarlarkTokenTypes.IN_KEYWORD, StarlarkBundle.message("parser.expected.in"))
    context.expressionParser.parseExpression()
    parseColonAndSuite()
    endMarker.done(StarlarkElementTypes.FOR_STATEMENT)
  }

  private fun parseColonAndSuite() {
    if (expectColon()) {
      parseSuite(null, null)
    } else {
      val mark = builder.mark()
      mark.done(StarlarkElementTypes.STATEMENT_LIST)
    }
  }

  fun parseSuite(endMarker: Marker?, elType: IElementType?) {
    if (atToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
      builder.advanceLexer()
      val marker = builder.mark()
      val indentFound = atToken(StarlarkTokenTypes.INDENT)
      if (indentFound) {
        builder.advanceLexer()
        while (!builder.eof() && !atToken(StarlarkTokenTypes.DEDENT)) {
          parseStatement()
        }
      } else {
        builder.error(StarlarkBundle.message("parser.indent.expected"))
      }
      marker.done(StarlarkElementTypes.STATEMENT_LIST)
      endMarker?.done(elType!!)
      if (indentFound && !builder.eof()) {
        assert(atToken(StarlarkTokenTypes.DEDENT))
        builder.advanceLexer()
      }
    } else {
      val marker = builder.mark()
      if (builder.eof()) {
        builder.error(StarlarkBundle.message("parser.expected.statement"))
      } else {
        context.pushScope(context.getScope().withSuite())
        parseSimpleStatement()
        context.popScope()
        while (matchToken(StarlarkTokenTypes.SEMICOLON)) {
          if (matchToken(StarlarkTokenTypes.STATEMENT_BREAK)) {
            break
          }
          context.pushScope(context.getScope().withSuite())
          parseSimpleStatement()
          context.popScope()
        }
      }
      marker.done(StarlarkElementTypes.STATEMENT_LIST)
      endMarker?.done(elType!!)
    }
  }

  private fun parseLoadStatement() {
    assertCurrentToken(StarlarkTokenTypes.LOAD_KEYWORD)
    val loadStatement = builder.mark()
    builder.advanceLexer()
    parseLoadValueList()
    checkEndOfStatement()
    loadStatement.done(StarlarkElementTypes.LOAD_STATEMENT)
  }

  private fun parseLoadValueList() {
    if (!atToken(StarlarkTokenTypes.LPAR)) {
      builder.error(StarlarkBundle.message("parser.expected.lpar"))
      return
    }
    builder.advanceLexer()
    var firstValue = true
    while (!atToken(StarlarkTokenTypes.RPAR)) {
      if (!firstValue) {
        if (matchToken(StarlarkTokenTypes.COMMA)) {
          if (atToken(StarlarkTokenTypes.RPAR)) {
            break
          }
        } else {
          builder.error(StarlarkBundle.message("parser.expected.comma.or.rpar"))
          break
        }
      }

      if (firstValue) {
        firstValue = false
        parseLoadValue()
        continue
      } else if (isIdentifierLike(builder)) {
        val namedLoadValueMarker = builder.mark()
        advanceIdentifierLike(builder)
        if (atToken(StarlarkTokenTypes.EQ)) {
          builder.advanceLexer()
          parseLoadValue()
          namedLoadValueMarker.done(StarlarkElementTypes.NAMED_LOAD_VALUE)
          continue
        }
        namedLoadValueMarker.rollbackTo()
      }
      val loadValueMarker = builder.mark()
      parseLoadValue()
      loadValueMarker.done(StarlarkElementTypes.STRING_LOAD_VALUE)
    }
    checkMatches(StarlarkTokenTypes.RPAR, StarlarkBundle.message("parser.expected.rpar"))
  }

  private fun parseLoadValue() =
    if (atToken(StarlarkTokenTypes.STRING)) {
      buildTokenElement(StarlarkElementTypes.STRING_LITERAL_EXPRESSION, builder)
    } else {
      advanceError(builder, StarlarkBundle.message("parser.expected.string"))
    }

  private fun parseExpressionStatement(firstToken: IElementType) {
    var exprStatement = builder.mark()
    if (context.expressionParser.parseExpressionOptional()) {
      var statementType = StarlarkElementTypes.EXPRESSION_STATEMENT
      if (StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS.contains(builder.tokenType)) {
        statementType = StarlarkElementTypes.AUG_ASSIGNMENT_STATEMENT
        builder.advanceLexer()
        if (!context.expressionParser.parseExpressionOptional()) {
          builder.error(StarlarkBundle.message("parser.expected.expression"))
        }
      } else if (atToken(StarlarkTokenTypes.EQ)) {
        exprStatement.rollbackTo()
        exprStatement = builder.mark()
        context.expressionParser.parseExpression(isTarget = true)
        assertCurrentToken(StarlarkTokenTypes.EQ)
        statementType = StarlarkElementTypes.ASSIGNMENT_STATEMENT
        builder.advanceLexer()
        while (true) {
          val maybeExprMarker = builder.mark()
          if (!context.expressionParser.parseExpressionOptional()) {
            maybeExprMarker.drop()
            builder.error(StarlarkBundle.message("parser.expected.expression"))
            break
          }
          if (atToken(StarlarkTokenTypes.EQ)) {
            maybeExprMarker.rollbackTo()
            context.expressionParser.parseExpression(isTarget = true)
            assertCurrentToken(StarlarkTokenTypes.EQ)
            builder.advanceLexer()
          } else {
            maybeExprMarker.drop()
            break
          }
        }
      }
      checkEndOfStatement()
      exprStatement.done(statementType)
      return
    } else {
      exprStatement.drop()
    }
    builder.advanceLexer()
    reportParseStatementError(builder, firstToken)
  }
}
