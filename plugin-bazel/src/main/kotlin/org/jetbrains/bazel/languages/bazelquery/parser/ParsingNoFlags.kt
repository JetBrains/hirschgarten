package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes

open class ParsingNoFlags(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {
  private val utils = ParsingUtils(builder)

  companion object {
    private val wordInSqQuery =
      TokenSet.create(
        BazelqueryTokenTypes.UNQUOTED_WORD,
        BazelqueryTokenTypes.DQ_WORD,
        BazelqueryTokenTypes.DQ_UNFINISHED,
        BazelqueryTokenTypes.DQ_EMPTY,
      )

    private val wordInDqQuery =
      TokenSet.create(
        BazelqueryTokenTypes.UNQUOTED_WORD,
        BazelqueryTokenTypes.SQ_WORD,
        BazelqueryTokenTypes.SQ_UNFINISHED,
        BazelqueryTokenTypes.SQ_EMPTY,
      )

    private val wordInUnqQuery =
      TokenSet.create(
        BazelqueryTokenTypes.UNQUOTED_WORD,
        BazelqueryTokenTypes.SQ_WORD,
        BazelqueryTokenTypes.SQ_UNFINISHED,
        BazelqueryTokenTypes.SQ_EMPTY,
        BazelqueryTokenTypes.DQ_WORD,
        BazelqueryTokenTypes.DQ_UNFINISHED,
        BazelqueryTokenTypes.DQ_EMPTY,
      )
  }

  private fun getAvailableWordsSet(queryQuotes: IElementType): TokenSet =
    when (queryQuotes) {
      BazelqueryTokenTypes.WHITE_SPACE -> wordInUnqQuery
      BazelqueryTokenTypes.SINGLE_QUOTE -> wordInSqQuery
      BazelqueryTokenTypes.DOUBLE_QUOTE -> wordInDqQuery
      else -> TokenSet.EMPTY
    }

  fun parseFile(): ASTNode {
    val prompt = builder.mark()

    parseExpr(BazelqueryTokenTypes.WHITE_SPACE)

    while (!eof()) {
      utils.advanceError("Unexpected token")
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

      utils.atToken(BazelqueryTokenTypes.LET) -> {
        advanceLexer()
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelqueryTokenTypes.UNQUOTED_WORD)
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelqueryTokenTypes.EQUALS)
        if (utils.atToken(queryQuotes)) return queryQuotes
        if (utils.atToken(BazelqueryTokenTypes.IN)) error("expected value of variable in let expression")
        queryQuotes = parseExpr(queryQuotes)
        utils.expectToken(BazelqueryTokenTypes.IN)
        if (utils.atToken(queryQuotes)) return queryQuotes
        queryQuotes = parseExpr(queryQuotes)
      }

      utils.atToken(BazelqueryTokenTypes.LEFT_PAREN) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelqueryTokenTypes.RIGHT_PAREN)
      }

      utils.atToken(BazelqueryTokenTypes.SET) -> {
        advanceLexer()
        if (utils.atToken(queryQuotes)) return queryQuotes
        utils.expectToken(BazelqueryTokenTypes.LEFT_PAREN)
        if (!utils.matchAnyToken(getAvailableWordsSet(queryQuotes))) {
          utils.advanceError("expected target pattern as an argument of set expression")
        }
        while (!utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) && !utils.atToken(queryQuotes) && !eof()) {
          if (!utils.matchAnyToken(getAvailableWordsSet(queryQuotes))) {
            utils.advanceError("expected target pattern as an argument of set expression")
          }
        }
        if (!utils.matchToken(BazelqueryTokenTypes.RIGHT_PAREN)) error("<right parenthesis> expected")
      }

      utils.atAnyToken(BazelqueryTokenSets.COMMANDS) -> queryQuotes = parseCommand(queryQuotes)

      utils.atToken(queryQuotes) -> return queryQuotes

      utils.atToken(BazelqueryTokenTypes.UNION) ||
        utils.atToken(BazelqueryTokenTypes.EXCEPT) ||
        utils.atToken(BazelqueryTokenTypes.INTERSECT) ->
        utils.advanceError("unexpected token: infix operator at the beginning of expression")

      utils.atToken(BazelqueryTokenTypes.INTEGER) -> utils.advanceError("unexpected token: <integer>")

      utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) -> utils.advanceError("unexpected token: parenthesis mismatch")
    }

    when {
      utils.atToken(BazelqueryTokenTypes.INTERSECT) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
      }
      utils.atToken(BazelqueryTokenTypes.EXCEPT) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
      }
      utils.atToken(BazelqueryTokenTypes.UNION) -> {
        advanceLexer()
        queryQuotes = parseExpr(queryQuotes)
      }
    }

    return queryQuotes
  }

// _____parsing commands____

  private fun parseExprArg(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) ||
      utils.atToken(BazelqueryTokenTypes.COMMA) ||
      eof()
    ) {
      error("<expression> expected")
    } else if (utils.atToken(BazelqueryTokenTypes.INTEGER)) {
      utils.advanceError("<expression> expected, got <integer>")
    } else {
      queryQuotes = parseExpr(queryQuotes)
    }
    return queryQuotes
  }

  private fun parseWordArg(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) ||
      utils.atToken(BazelqueryTokenTypes.COMMA) ||
      eof()
    ) {
      error("<word> expected")
    } else if (!utils.atAnyToken(getAvailableWordsSet(queryQuotes))) {
      utils.advanceError("<word> expected")
    } else {
      queryQuotes = parseWord(queryQuotes)
    }

    return queryQuotes
  }

  private fun parseAsWordArg(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) ||
      utils.atToken(BazelqueryTokenTypes.COMMA) ||
      eof()
    ) {
      error("<word> expected")
    } else if (!utils.atAnyToken(getAvailableWordsSet(queryQuotes)) &&
      !utils.atToken(BazelqueryTokenTypes.INTEGER)
    ) {
      utils.advanceError("<word> expected")
    } else {
      queryQuotes = parseWord(queryQuotes)
    }

    return queryQuotes
  }

  private fun parseOptionalIntArg(queryQuotes: IElementType) {
    if (utils.matchToken(BazelqueryTokenTypes.COMMA)) {
      if (utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) ||
        utils.atToken(BazelqueryTokenTypes.COMMA) ||
        eof()
      ) {
        error("<expression> expected")
      } else if (!utils.atToken(BazelqueryTokenTypes.INTEGER)) {
        utils.advanceError("<integer> expected")
      } else {
        parseInteger()
      }
    }
  }

  private fun parseUnknownArgs(queryQuotes: IElementType): IElementType {
    var queryQuotes = queryQuotes
    if (utils.atToken(BazelqueryTokenTypes.INTEGER)) {
      parseInteger()
    } else if (!utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN)) {
      parseExpr(queryQuotes)
    }
    while (!utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) && !utils.atToken(queryQuotes) && !eof()) {
      utils.expectToken(BazelqueryTokenTypes.COMMA)
      if (utils.atToken(queryQuotes) ||
        utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN) ||
        eof()
      ) {
        error("<expression> expected")
      } else if (utils.atToken(BazelqueryTokenTypes.INTEGER)) {
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
    utils.expectToken(BazelqueryTokenTypes.LEFT_PAREN)
    if (utils.atToken(queryQuotes)) return queryQuotes

    when (commandKeyword) {
      BazelqueryTokenTypes.SAME_PKG_DIRECT_RDEPS,
      BazelqueryTokenTypes.SIBLINGS,
      BazelqueryTokenTypes.TESTS,
      BazelqueryTokenTypes.BUILDFILES,
      BazelqueryTokenTypes.LOADFILES,
      -> {
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelqueryTokenTypes.ALLPATHS,
      BazelqueryTokenTypes.SOMEPATH,
      BazelqueryTokenTypes.VISIBLE,
      -> {
        queryQuotes = parseExprArg(queryQuotes)
        if (!utils.matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelqueryTokenTypes.LABELS,
      BazelqueryTokenTypes.KIND,
      BazelqueryTokenTypes.FILTER,
      -> {
        queryQuotes = parseWordArg(queryQuotes)
        if (!utils.matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelqueryTokenTypes.DEPS,
      BazelqueryTokenTypes.ALLRDEPS,
      BazelqueryTokenTypes.SOME,
      -> {
        queryQuotes = parseExprArg(queryQuotes)
        parseOptionalIntArg(queryQuotes)
      }

      BazelqueryTokenTypes.RDEPS -> {
        queryQuotes = parseExprArg(queryQuotes)
        if (!utils.matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
        queryQuotes = parseExprArg(queryQuotes)
        parseOptionalIntArg(queryQuotes)
      }

      BazelqueryTokenTypes.ATTR -> {
        queryQuotes = parseWordArg(queryQuotes)
        if (!utils.matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
        queryQuotes = parseAsWordArg(queryQuotes)
        if (!utils.matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
        queryQuotes = parseExprArg(queryQuotes)
      }

      BazelqueryTokenTypes.RBUILDFILES -> {
        queryQuotes = parseWordArg(queryQuotes)
        while (utils.atToken(BazelqueryTokenTypes.COMMA) && !eof()) {
          if (!utils.matchToken(BazelqueryTokenTypes.COMMA)) utils.advanceError("<comma> expected")
          queryQuotes = parseWordArg(queryQuotes)
        }
      }
      else -> queryQuotes = parseUnknownArgs(queryQuotes)
    }

    if (eof() || utils.atToken(queryQuotes)) error("<right parenthesis> expected")
    while (!eof() && !utils.atToken(queryQuotes) && !utils.atToken(BazelqueryTokenTypes.RIGHT_PAREN)) {
      utils.advanceError("too many arguments")
    }
    if (!utils.matchToken(BazelqueryTokenTypes.RIGHT_PAREN)) {
      error("<right parenthesis> expected")
    }
    command.done(BazelqueryElementTypes.COMMAND)

    return queryQuotes
  }

  // _____parsing words____

  private fun parseWord(queryQuotes: IElementType): IElementType {
    val word = mark()
    var queryQuotes = queryQuotes

    if (utils.atToken(BazelqueryTokenTypes.SQ_EMPTY) ||
      utils.atToken(BazelqueryTokenTypes.DQ_EMPTY)
    ) {
      utils.advanceError("empty quotation")
    } else if (utils.matchToken(BazelqueryTokenTypes.SQ_UNFINISHED) ||
      utils.matchToken(BazelqueryTokenTypes.DQ_UNFINISHED)
    ) {
      error("<quote> expected")
    } else {
      if (queryQuotes == BazelqueryTokenTypes.WHITE_SPACE) {
        if (utils.atToken(BazelqueryTokenTypes.SQ_WORD)) {
          queryQuotes = BazelqueryTokenTypes.DOUBLE_QUOTE
        } else if (utils.atToken(BazelqueryTokenTypes.DQ_WORD)) {
          queryQuotes = BazelqueryTokenTypes.SINGLE_QUOTE
        }
      }
      advanceLexer()
    }
    word.done(BazelqueryElementTypes.WORD)
    return queryQuotes
  }

  // _____parsing integers____

  private fun parseInteger() {
    val integer = mark()

    if (utils.atToken(BazelqueryTokenTypes.INTEGER)) {
      advanceLexer()
    } else {
      utils.advanceError("<integer> expected")
    }

    integer.done(BazelqueryElementTypes.INTEGER)
  }
}
