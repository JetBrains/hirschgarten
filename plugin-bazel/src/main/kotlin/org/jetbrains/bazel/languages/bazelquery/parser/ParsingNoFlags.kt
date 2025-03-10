package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenType


open class Parsing(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {

  companion object {

    private val wordInSqQuery = TokenSet.create(
      BazelqueryTokenTypes.UNQUOTED_WORD,
      BazelqueryTokenTypes.DQ_WORD,
      BazelqueryTokenTypes.DQ_UNFINISHED,
      BazelqueryTokenTypes.DQ_EMPTY
    )

    private val wordInDqQuery = TokenSet.create(
      BazelqueryTokenTypes.UNQUOTED_WORD,
      BazelqueryTokenTypes.SQ_WORD,
      BazelqueryTokenTypes.SQ_UNFINISHED,
      BazelqueryTokenTypes.SQ_EMPTY
    )

    private val wordInUnqQuery = TokenSet.create(
      BazelqueryTokenTypes.UNQUOTED_WORD,

      BazelqueryTokenTypes.SQ_WORD,
      BazelqueryTokenTypes.SQ_UNFINISHED,
      BazelqueryTokenTypes.SQ_EMPTY,
      BazelqueryTokenTypes.DQ_WORD,
      BazelqueryTokenTypes.DQ_UNFINISHED,
      BazelqueryTokenTypes.DQ_EMPTY
    )

  }

  private fun getAvailableWordsSet(queryQuotes: IElementType): TokenSet {
    return when (queryQuotes) {
      BazelqueryTokenTypes.WHITE_SPACE -> wordInUnqQuery
      BazelqueryTokenTypes.SINGLE_QUOTE -> wordInSqQuery
      BazelqueryTokenTypes.DOUBLE_QUOTE -> wordInDqQuery
      else -> TokenSet.EMPTY
    }
  }


  // _____Matching and advancing_____

  private fun atToken(tokenType: IElementType?): Boolean = builder.tokenType === tokenType

  private fun atAnyToken(tokenTypes: TokenSet): Boolean = tokenTypes.contains(tokenType)

  private fun matchToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      advanceLexer()
      return true
    }
    return false
  }

  private fun expectToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      advanceLexer()
      return true
    }
    advanceError("<$tokenType> expected")
    return false
  }

  private fun matchAnyToken(tokens: TokenSet): Boolean {
    if (atAnyToken(tokens)) {
      advanceLexer()
      return true
    }
    return false
  }

  private fun advanceError(message: String) {
    val err = mark()
    advanceLexer()
    err.error(message)
  }


  // _____Parsing_____

  fun parseFile(): ASTNode {
    val prompt = builder.mark()

    parseExpr(BazelqueryTokenTypes.WHITE_SPACE)

    while (!eof())
      advanceError("Unexpected token")

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
  private fun parseExpr(queryQuotes: IElementType) {
    var queryQuotes = queryQuotes
    when {
      atAnyToken(getAvailableWordsSet(queryQuotes)) -> queryQuotes = parseWord(queryQuotes)

      atToken(BazelqueryTokenTypes.LET) -> {
        advanceLexer()
        if (atToken(queryQuotes)) return
        expectToken(BazelqueryTokenTypes.UNQUOTED_WORD)
        if (atToken(queryQuotes)) return
        expectToken(BazelqueryTokenTypes.EQUALS)
        if (atToken(queryQuotes)) return
        if (atToken(BazelqueryTokenTypes.IN)) error("<expression> expected")
        parseExpr(queryQuotes)
        expectToken(BazelqueryTokenTypes.IN)
        if (atToken(queryQuotes)) return
        parseExpr(queryQuotes)
      }

      atToken(BazelqueryTokenTypes.LEFT_PAREN) -> {
        advanceLexer()
        parseExpr(queryQuotes)
        if (atToken(queryQuotes)) return
        expectToken(BazelqueryTokenTypes.RIGHT_PAREN)
      }

      atToken(BazelqueryTokenTypes.SET) -> {
        advanceLexer()
        if (atToken(queryQuotes)) return
        expectToken(BazelqueryTokenTypes.LEFT_PAREN)
        if(!matchAnyToken(getAvailableWordsSet(queryQuotes))) advanceError("<word> expected")
        while (!atToken(BazelqueryTokenTypes.RIGHT_PAREN) && !atToken(queryQuotes) && !eof()) {
          if(!matchAnyToken(getAvailableWordsSet(queryQuotes))) advanceError("<word> expected")
        }
        if(!matchToken(BazelqueryTokenTypes.RIGHT_PAREN)) error("<right parenthesis> expected")
      }

      atAnyToken(BazelqueryTokenSets.COMMANDS) -> parseCommand(queryQuotes)

      atToken(queryQuotes) -> return

      atToken(BazelqueryTokenTypes.UNION) -> advanceError("Unexpected token in query value")
      atToken(BazelqueryTokenTypes.EXCEPT) -> advanceError("Unexpected token in query value")
      atToken(BazelqueryTokenTypes.INTERSECT) -> advanceError("Unexpected token in query value")

      atToken(BazelqueryTokenTypes.INTEGER) -> advanceError("Unexpected token in query value")

      atToken(BazelqueryTokenTypes.RIGHT_PAREN) -> advanceError("Unexpected token in query value")
    }

    when {
      atToken(BazelqueryTokenTypes.INTERSECT) -> {advanceLexer(); parseExpr(queryQuotes)}
      atToken(BazelqueryTokenTypes.EXCEPT) -> {advanceLexer(); parseExpr(queryQuotes)}
      atToken(BazelqueryTokenTypes.UNION) -> {advanceLexer(); parseExpr(queryQuotes)}
    }
  }


// _____parsing commands____

  private fun parseExprArg(queryQuotes: IElementType) {
    if (atToken(BazelqueryTokenTypes.RIGHT_PAREN) || atToken(BazelqueryTokenTypes.COMMA) || eof()) error("<expression> expected")
    else if(atToken(BazelqueryTokenTypes.INTEGER)) advanceError("<expression> expected, got <integer>")
    else parseExpr(queryQuotes)
  }

  private fun parseWordArg(queryQuotes: IElementType) : IElementType {
    var queryQuotes = queryQuotes
    if (atToken(BazelqueryTokenTypes.RIGHT_PAREN) || atToken(BazelqueryTokenTypes.COMMA) || eof()) error("<word> expected")
    else if(!atAnyToken(getAvailableWordsSet(queryQuotes))) advanceError("<word> expected")
    else queryQuotes = parseWord(queryQuotes)

    return queryQuotes
  }

  private fun parseAsWordArg(queryQuotes: IElementType) : IElementType {
    var queryQuotes = queryQuotes
    if (atToken(BazelqueryTokenTypes.RIGHT_PAREN) || atToken(BazelqueryTokenTypes.COMMA) || eof()) error("<word> expected")
    else if(!atAnyToken(getAvailableWordsSet(queryQuotes)) && !atToken(BazelqueryTokenTypes.INTEGER)) advanceError("<word> expected")
    else queryQuotes = parseWord(queryQuotes)

    return queryQuotes
  }

  private fun parseOptionalIntArg(queryQuotes: IElementType) {
    if(matchToken(BazelqueryTokenTypes.COMMA)) {
      if (atToken(BazelqueryTokenTypes.RIGHT_PAREN) || atToken(BazelqueryTokenTypes.COMMA) || eof()) error("<expression> expected")
      else if(!atToken(BazelqueryTokenTypes.INTEGER)) advanceError("<integer> expected")
      else parseInteger()
    }
  }

  private fun parseUnknownArgs(queryQuotes: IElementType){
    if (atToken(BazelqueryTokenTypes.INTEGER)) parseInteger()
    else if (!atToken(BazelqueryTokenTypes.RIGHT_PAREN)) parseExpr(queryQuotes)
    while (!atToken(BazelqueryTokenTypes.RIGHT_PAREN) && !atToken(queryQuotes) && !eof()) {
      expectToken(BazelqueryTokenTypes.COMMA)
      if (atToken(queryQuotes) || atToken(BazelqueryTokenTypes.RIGHT_PAREN) || eof()) error("<expression> expected")
      else if (atToken(BazelqueryTokenTypes.INTEGER)) parseInteger()
      else parseExpr(queryQuotes)
    }
  }

  private fun parseCommand(queryQuotes: IElementType) : IElementType {
    var queryQuotes = queryQuotes

    val command = mark()

    val command_keyword = builder.tokenType
    advanceLexer()
    if (atToken(queryQuotes)) return queryQuotes
    expectToken(BazelqueryTokenTypes.LEFT_PAREN)
    if (atToken(queryQuotes)) return queryQuotes

    if (
      command_keyword == BazelqueryTokenTypes.SAME_PKG_DIRECT_RDEPS ||
      command_keyword == BazelqueryTokenTypes.SIBLINGS ||
      command_keyword == BazelqueryTokenTypes.TESTS ||
      command_keyword == BazelqueryTokenTypes.BUILDFILES ||
      command_keyword == BazelqueryTokenTypes.LOADFILES
    ) {
      parseExprArg(queryQuotes)
    }
    else if (
      command_keyword == BazelqueryTokenTypes.ALLPATHS ||
      command_keyword == BazelqueryTokenTypes.SOMEPATH ||
      command_keyword == BazelqueryTokenTypes.VISIBLE
    ) {
      parseExprArg(queryQuotes)
      if(!matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
      parseExprArg(queryQuotes)
    }
    else if (
      command_keyword == BazelqueryTokenTypes.LABELS ||
      command_keyword == BazelqueryTokenTypes.KIND ||
      command_keyword == BazelqueryTokenTypes.FILTER
    ) {
      queryQuotes = parseWordArg(queryQuotes)
      if(!matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
      parseExprArg(queryQuotes)
    }
    else if (
      command_keyword == BazelqueryTokenTypes.DEPS ||
      command_keyword == BazelqueryTokenTypes.ALLRDEPS ||
      command_keyword == BazelqueryTokenTypes.SOME
    ) {
      parseExprArg(queryQuotes)
      parseOptionalIntArg(queryQuotes)
    }
    else if (
      command_keyword == BazelqueryTokenTypes.RDEPS
    ) {
      parseExprArg(queryQuotes)
      if(!matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
      parseExprArg(queryQuotes)
      parseOptionalIntArg(queryQuotes)
    }
    else if (
      command_keyword == BazelqueryTokenTypes.ATTR
    ) {
      queryQuotes = parseWordArg(queryQuotes)
      if(!matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
      queryQuotes = parseAsWordArg(queryQuotes)
      if(!matchToken(BazelqueryTokenTypes.COMMA)) error("<comma> expected")
      parseExprArg(queryQuotes)
    }
    else if (
      command_keyword == BazelqueryTokenTypes.RBUILDFILES
    ) {
      queryQuotes = parseWordArg(queryQuotes)
      while (atToken(BazelqueryTokenTypes.COMMA) && !eof()) {
        if (!matchToken(BazelqueryTokenTypes.COMMA)) advanceError("<comma> expected")
        queryQuotes = parseWordArg(queryQuotes)
      }
    }
    else parseUnknownArgs(queryQuotes)

    if (eof() || atToken(queryQuotes)) error("<right parenthesis> expected")
    while(!eof() && !atToken(queryQuotes) && !atToken(BazelqueryTokenTypes.RIGHT_PAREN)) advanceError("too many arguments")
    if(!matchToken(BazelqueryTokenTypes.RIGHT_PAREN)) error("<right parenthesis> expected")
    command.done(BazelqueryElementTypes.COMMAND)

    return queryQuotes
  }


  // _____parsing words____

  private fun parseWord(queryQuotes: IElementType): IElementType {
    val word = mark()
    var queryQuotes = queryQuotes

    if(atToken(BazelqueryTokenTypes.SQ_EMPTY) || atToken(BazelqueryTokenTypes.DQ_EMPTY)) advanceError("Empty quote")
    else if(matchToken(BazelqueryTokenTypes.SQ_UNFINISHED) || matchToken(BazelqueryTokenTypes.DQ_UNFINISHED)) error("Quote expected")
    else {
      if (queryQuotes == BazelqueryTokenTypes.WHITE_SPACE) {
        if (atToken(BazelqueryTokenTypes.SQ_WORD))
          queryQuotes = BazelqueryTokenTypes.DOUBLE_QUOTE
        else if (atToken(BazelqueryTokenTypes.DQ_WORD))
          queryQuotes = BazelqueryTokenTypes.SINGLE_QUOTE
      }
      advanceLexer()
    }
    word.done(BazelqueryElementTypes.WORD)
    return queryQuotes
  }


  // _____parsing integers____

  private fun parseInteger() {
    val integer = mark()

    if(atToken(BazelqueryTokenTypes.INTEGER)) advanceLexer()
    else advanceError("Integer expected")

    integer.done(BazelqueryElementTypes.INTEGER)
  }
}

