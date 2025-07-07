package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes

open class ParsingNoFlags(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {
  private val utils = ParsingUtils(builder)

  companion object {
    private val wordInSqQuery =
      TokenSet.create(
        BazelQueryTokenTypes.UNQUOTED_WORD,
        BazelQueryTokenTypes.DQ_WORD,
        BazelQueryTokenTypes.DQ_UNFINISHED,
        BazelQueryTokenTypes.DQ_EMPTY,
        BazelQueryTokenTypes.ERR_WORD,
      )

    private val wordInDqQuery =
      TokenSet.create(
        BazelQueryTokenTypes.UNQUOTED_WORD,
        BazelQueryTokenTypes.SQ_WORD,
        BazelQueryTokenTypes.SQ_UNFINISHED,
        BazelQueryTokenTypes.SQ_EMPTY,
        BazelQueryTokenTypes.ERR_WORD,
      )

    private val wordInUnqQuery =
      TokenSet.create(
        BazelQueryTokenTypes.UNQUOTED_WORD,
        BazelQueryTokenTypes.SQ_WORD,
        BazelQueryTokenTypes.SQ_UNFINISHED,
        BazelQueryTokenTypes.SQ_EMPTY,
        BazelQueryTokenTypes.DQ_WORD,
        BazelQueryTokenTypes.DQ_UNFINISHED,
        BazelQueryTokenTypes.DQ_EMPTY,
        BazelQueryTokenTypes.ERR_WORD,
      )

    private val patternInSqQuery =
      TokenSet.create(
        *wordInSqQuery.types,
        BazelQueryTokenTypes.DQ_PATTERN,
      )

    private val patternInDqQuery =
      TokenSet.create(
        *wordInDqQuery.types,
        BazelQueryTokenTypes.SQ_PATTERN,
      )

    private val patternInUnqQuery =
      TokenSet.create(
        *wordInUnqQuery.types,
        BazelQueryTokenTypes.SQ_PATTERN,
        BazelQueryTokenTypes.DQ_PATTERN,
      )
  }

  private fun getAvailableWordsSet(queryQuotes: IElementType): TokenSet =
    when (queryQuotes) {
      BazelQueryTokenTypes.WHITE_SPACE -> wordInUnqQuery
      BazelQueryTokenTypes.SINGLE_QUOTE -> wordInSqQuery
      BazelQueryTokenTypes.DOUBLE_QUOTE -> wordInDqQuery
      else -> TokenSet.EMPTY
    }

  private fun getAvailablePatternsSet(queryQuotes: IElementType): TokenSet =
    when (queryQuotes) {
      BazelQueryTokenTypes.WHITE_SPACE -> patternInUnqQuery
      BazelQueryTokenTypes.SINGLE_QUOTE -> patternInSqQuery
      BazelQueryTokenTypes.DOUBLE_QUOTE -> patternInDqQuery
      else -> TokenSet.EMPTY
    }

  fun parseFile(): ASTNode {
    val prompt = builder.mark()

    parseExpr(BazelQueryTokenTypes.WHITE_SPACE)

    while (!eof()) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.unexpected.token"))
    }

    prompt.done(root)
    return treeBuilt
  }

  // _____parsing queries____

  // expr ::= word
  //       | let name = expr in expr
  //       | (expr)
  //       | expr intersect expr
  //       | expr ^ expr
  //       | expr union expr
  //       | expr + expr
  //       | expr except expr
  //       | expr - expr
  //       | set(word *)
  //       | word '(' int | word | expr ... ')'
  private fun parseExpr(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    when {
      utils.atAnyToken(getAvailableWordsSet(queryQuotes)) -> queryQuotes = parseWord(queryQuotes)

      utils.atToken(BazelQueryTokenTypes.LET) -> {
        advanceLexer()
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelQueryTokenTypes.UNQUOTED_WORD)
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelQueryTokenTypes.EQUALS)
        if (utils.atToken(queryQuotes)) return queryQuotes
        if (utils.atToken(BazelQueryTokenTypes.IN)) error(BazelPluginBundle.message("bazelquery.error.missing.variable.value"))
        queryQuotes = parseExpr(queryQuotes)
        utils.expectToken(BazelQueryTokenTypes.IN)
        if (utils.atToken(queryQuotes)) return queryQuotes
        queryQuotes = parseExpr(queryQuotes)
      }

      utils.atToken(BazelQueryTokenTypes.LEFT_PAREN) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelQueryTokenTypes.RIGHT_PAREN)
      }

      utils.atToken(BazelQueryTokenTypes.SET) -> {
        advanceLexer()
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelQueryTokenTypes.LEFT_PAREN)
        if (!utils.matchAnyToken(getAvailableWordsSet(queryQuotes))) {
          utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.target"))
        }
        while (!utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) && !utils.atToken(queryQuotes) && !eof()) {
          if (!utils.matchAnyToken(getAvailableWordsSet(queryQuotes))) {
            utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.target"))
          }
        }
        if (!utils.matchToken(
            BazelQueryTokenTypes.RIGHT_PAREN,
          )
        ) {
          error(BazelPluginBundle.message("bazelquery.error.missing.right.parenthesis"))
        }
      }

      utils.atAnyToken(BazelQueryTokenSets.COMMANDS) -> queryQuotes = parseCommand(queryQuotes)

      utils.atToken(queryQuotes) -> return queryQuotes

      utils.atToken(BazelQueryTokenTypes.UNION) ||
        utils.atToken(BazelQueryTokenTypes.EXCEPT) ||
        utils.atToken(BazelQueryTokenTypes.INTERSECT) ->
        utils.advanceError(BazelPluginBundle.message("bazelquery.error.unexpected.token.infix.operator"))

      utils.atToken(BazelQueryTokenTypes.INTEGER) ->
        utils.advanceError(BazelPluginBundle.message("bazelquery.error.unexpected.token.integer"))

      utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ->
        utils.advanceError(BazelPluginBundle.message("bazelquery.error.unexpected.token.paren"))
    }

    when {
      utils.atToken(BazelQueryTokenTypes.INTERSECT) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
      }
      utils.atToken(BazelQueryTokenTypes.EXCEPT) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
      }
      utils.atToken(BazelQueryTokenTypes.UNION) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
      }
    }

    return queryQuotes
  }

// _____parsing commands____

  private fun parseExprArg(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
      utils.atToken(BazelQueryTokenTypes.COMMA) ||
      eof()
    ) {
      error(BazelPluginBundle.message("bazelquery.error.missing.expression"))
    } else if (utils.atToken(BazelQueryTokenTypes.INTEGER)) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.expression.got.integer"))
    } else if (utils.atToken(BazelQueryTokenTypes.SQ_PATTERN) ||
        utils.atToken(BazelQueryTokenTypes.DQ_PATTERN)) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.incorrect.word"))
    }else {
      queryQuotes = parseExpr(queryQuotes)
    }
    return queryQuotes
  }

  private fun parseWordArg(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
      utils.atToken(BazelQueryTokenTypes.COMMA) ||
      eof()
    ) {
      error(BazelPluginBundle.message("bazelquery.error.missing.word"))
    } else if (!utils.atAnyToken(getAvailableWordsSet(queryQuotes))) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.word"))
    } else {
      queryQuotes = parseWord(queryQuotes)
    }

    return queryQuotes
  }

  private fun parsePatternArg(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
      utils.atToken(BazelQueryTokenTypes.COMMA) ||
      eof()
    ) {
      error(BazelPluginBundle.message("bazelquery.error.missing.word"))
    } else if (!utils.atAnyToken(getAvailablePatternsSet(queryQuotes)) &&
      !utils.atToken(BazelQueryTokenTypes.INTEGER)
    ) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.word"))
    } else {
      queryQuotes = parseWord(queryQuotes)
    }

    return queryQuotes
  }

  private fun parseOptionalIntArg(queryQuotes: IElementType) {
    if (utils.matchToken(BazelQueryTokenTypes.COMMA)) {
      if (utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
        utils.atToken(BazelQueryTokenTypes.COMMA) ||
        eof()
      ) {
        error(BazelPluginBundle.message("bazelquery.error.missing.expression"))
      } else if (!utils.atToken(BazelQueryTokenTypes.INTEGER)) {
        utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.integer"))
      } else {
        parseInteger()
      }
    }
  }

  private fun parseUnknownArgs(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelQueryTokenTypes.INTEGER)) {
      parseInteger()
    } else if (!utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN)) {
      parseExpr(queryQuotes)
    }
    while (!utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) && !utils.atToken(queryQuotes) && !eof()) {
      utils.expectToken(BazelQueryTokenTypes.COMMA)
      if (utils.atToken(queryQuotes) ||
        utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN) ||
        eof()
      ) {
        error(BazelPluginBundle.message("bazelquery.error.missing.expression"))
      } else if (utils.atToken(BazelQueryTokenTypes.INTEGER)) {
        parseInteger()
      } else {
        queryQuotes = parseExpr(queryQuotes)
      }
    }
    return queryQuotes
  }

  private fun parseCommand(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes

    val command = mark()

    val commandKeyword = builder.tokenType
    advanceLexer()
    if (utils.atToken(queryQuotes)) return queryQuotes
    utils.expectToken(BazelQueryTokenTypes.LEFT_PAREN)
    if (utils.atToken(queryQuotes)) return queryQuotes

    when (commandKeyword) {
      BazelQueryTokenTypes.SAME_PKG_DIRECT_RDEPS,
      BazelQueryTokenTypes.SIBLINGS,
      BazelQueryTokenTypes.TESTS,
      BazelQueryTokenTypes.BUILDFILES,
      BazelQueryTokenTypes.LOADFILES,
      -> {
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelQueryTokenTypes.ALLPATHS,
      BazelQueryTokenTypes.SOMEPATH,
      BazelQueryTokenTypes.VISIBLE,
      -> {
        queryQuotes = parseExprArg(queryQuotes)
        if (!utils.matchToken(BazelQueryTokenTypes.COMMA)) error(BazelPluginBundle.message("bazelquery.error.missing.comma"))
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelQueryTokenTypes.LABELS,
      BazelQueryTokenTypes.KIND,
      BazelQueryTokenTypes.FILTER,
      -> {
        queryQuotes = parsePatternArg(queryQuotes)
        if (!utils.matchToken(BazelQueryTokenTypes.COMMA)) error(BazelPluginBundle.message("bazelquery.error.missing.comma"))
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelQueryTokenTypes.DEPS,
      BazelQueryTokenTypes.ALLRDEPS,
      BazelQueryTokenTypes.SOME,
      -> {
        queryQuotes = parseExprArg(queryQuotes)
        parseOptionalIntArg(queryQuotes)
      }

      BazelQueryTokenTypes.RDEPS -> {
        queryQuotes = parseExprArg(queryQuotes)
        if (!utils.matchToken(BazelQueryTokenTypes.COMMA)) error(BazelPluginBundle.message("bazelquery.error.missing.comma"))
        queryQuotes = parseExprArg(queryQuotes)
        parseOptionalIntArg(queryQuotes)
      }

      BazelQueryTokenTypes.ATTR -> {
        queryQuotes = parseWordArg(queryQuotes)
        if (!utils.matchToken(BazelQueryTokenTypes.COMMA)) error(BazelPluginBundle.message("bazelquery.error.missing.comma"))
        queryQuotes = parsePatternArg(queryQuotes)
        if (!utils.matchToken(BazelQueryTokenTypes.COMMA)) error(BazelPluginBundle.message("bazelquery.error.missing.comma"))
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelQueryTokenTypes.RBUILDFILES -> {
        queryQuotes = parseWordArg(queryQuotes)
        while (utils.atToken(BazelQueryTokenTypes.COMMA) && !eof()) {
          if (!utils.matchToken(BazelQueryTokenTypes.COMMA)) utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.comma"))
          queryQuotes = parseWordArg(queryQuotes)
        }
      }
      else -> queryQuotes = parseUnknownArgs(queryQuotes)
    }

    if (eof() || utils.atToken(queryQuotes)) error(BazelPluginBundle.message("bazelquery.error.missing.right.parenthesis"))
    while (!eof() && !utils.atToken(queryQuotes) && !utils.atToken(BazelQueryTokenTypes.RIGHT_PAREN)) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.unexpected.token"))
    }
    if (!utils.matchToken(BazelQueryTokenTypes.RIGHT_PAREN)) {
      error(BazelPluginBundle.message("bazelquery.error.missing.right.parenthesis"))
    }
    command.done(BazelQueryElementTypes.COMMAND)

    return queryQuotes
  }

  // _____parsing words____

  private fun parseWord(queryQuotes: IElementType): IElementType {
    val word = mark()
    var queryQuotes = queryQuotes

    if (utils.atToken(BazelQueryTokenTypes.SQ_EMPTY) ||
      utils.atToken(BazelQueryTokenTypes.DQ_EMPTY)
    ) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.empty.quotation"))
    } else if (utils.matchToken(BazelQueryTokenTypes.SQ_UNFINISHED) ||
      utils.matchToken(BazelQueryTokenTypes.DQ_UNFINISHED)
    ) {
      error(BazelPluginBundle.message("bazelquery.error.missing.quote"))
    } else if (utils.atToken(BazelQueryTokenTypes.ERR_WORD)) {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.incorrect.word"))
    } else {
      if (queryQuotes == BazelQueryTokenTypes.WHITE_SPACE) {
        if (utils.atToken(BazelQueryTokenTypes.SQ_WORD)) {
          queryQuotes = BazelQueryTokenTypes.DOUBLE_QUOTE
        } else if (utils.atToken(BazelQueryTokenTypes.DQ_WORD)) {
          queryQuotes = BazelQueryTokenTypes.SINGLE_QUOTE
        }
      }
      advanceLexer()
    }
    word.done(BazelQueryElementTypes.WORD)
    return queryQuotes
  }

  // _____parsing integers____

  private fun parseInteger() {
    val integer = mark()

    if (utils.atToken(BazelQueryTokenTypes.INTEGER)) {
      advanceLexer()
    } else {
      utils.advanceError(BazelPluginBundle.message("bazelquery.error.missing.integer"))
    }

    integer.done(BazelQueryElementTypes.INTEGER)
  }
}
