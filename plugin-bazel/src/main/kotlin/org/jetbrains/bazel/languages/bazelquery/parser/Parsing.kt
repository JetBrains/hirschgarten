package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets

open class Parsing(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {

  companion object {

    private val queryValPrefixes = TokenSet.create(
      BazelqueryTokenTypes.COMMAND,
      *BazelqueryTokenSets.WORDS.types,
      BazelqueryTokenTypes.SET,
      BazelqueryTokenTypes.LET,
      *BazelqueryTokenSets.QUOTES.types
    )

    private val queryValEnd = TokenSet.create(
      BazelqueryTokenTypes.BAZEL,
      BazelqueryTokenTypes.QUERY,
      BazelqueryTokenTypes.DOUBLE_HYPHEN
    )

    private val wordInSqQuery = TokenSet.create(
      BazelqueryTokenTypes.UNQUOTED_WORD,
      BazelqueryTokenTypes.DQ_WORD
    )

    private val wordInDqQuery = TokenSet.create(
      BazelqueryTokenTypes.UNQUOTED_WORD,
      BazelqueryTokenTypes.SQ_WORD
    )

    private val wordInUnqQuery = TokenSet.create(
      BazelqueryTokenTypes.UNQUOTED_WORD,
      BazelqueryTokenTypes.SQ_WORD,
      BazelqueryTokenTypes.DQ_WORD
    )

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
    val file = builder.mark()
    while (!eof()) {
      when {
        atToken(BazelqueryTokenTypes.BAZEL) -> parsePrompt()

        else -> advanceError("<bazel> expected")
      }
    }

    file.done(root)
    return treeBuilt
  }

  private fun parsePrompt() {
    val promptMaker = mark()

    advanceLexer()

    expectToken(BazelqueryTokenTypes.QUERY)

    while (!atAnyToken(queryValPrefixes) && !eof()) {
      when {
        atToken(BazelqueryTokenTypes.DOUBLE_HYPHEN) -> parseFlag()

        else -> advanceError("Unexpected token (flag expected1)")
      }
    }

    if (atAnyToken(queryValPrefixes)) parseQueryVal()

    while (!atToken(BazelqueryTokenTypes.BAZEL) && !eof()) {
      when {
        atToken(BazelqueryTokenTypes.DOUBLE_HYPHEN) -> parseFlag()
        else -> advanceError("Unexpected token (flag expected2)")
      }
    }

    promptMaker.done(BazelqueryElementTypes.PROMPT)
  }


  private fun getAvailableWordsSet(queryQuotes: IElementType): TokenSet {
    return when {
      queryQuotes == BazelqueryTokenTypes.WHITE_SPACE -> wordInUnqQuery
      queryQuotes == BazelqueryTokenTypes.SINGLE_QUOTE -> wordInSqQuery
      queryQuotes == BazelqueryTokenTypes.DOUBLE_QUOTE -> wordInDqQuery
      else -> TokenSet.EMPTY
    }
  }

  //TODO: Nawiasowania, odpowiednie przetwarzanie operacji (np. nie można zaczynać od +)
  private fun parseQueryVal() {
    val queryVal = mark()
    var queryQuotes: IElementType = BazelqueryTokenTypes.WHITE_SPACE

    if (atToken(BazelqueryTokenTypes.SINGLE_QUOTE)){
      queryQuotes = BazelqueryTokenTypes.SINGLE_QUOTE
      advanceLexer()
    }
    else if (atToken(BazelqueryTokenTypes.DOUBLE_QUOTE)) {
      queryQuotes = BazelqueryTokenTypes.DOUBLE_QUOTE
      advanceLexer()
    }

    while (!atAnyToken(queryValEnd) && !atToken(queryQuotes) && !eof()) {
      when {
        atAnyToken(getAvailableWordsSet(queryQuotes)) -> parseWord()

        atToken(BazelqueryTokenTypes.COMMAND) -> parseCommand(queryQuotes)

        atToken(BazelqueryTokenTypes.SET) -> {advanceLexer()} //parseSet()

        atToken(BazelqueryTokenTypes.LET) -> {advanceLexer()} //parseLet()

        atAnyToken(BazelqueryTokenSets.OPERATIONS) -> {advanceLexer()}  //parseOperation()

        atToken(BazelqueryTokenTypes.LEFT_PAREN) -> {advanceLexer()}
        atToken(BazelqueryTokenTypes.RIGHT_PAREN) -> {
          // czy potrzeba licznika zagnieżdżeń? (na pierwszym poziomie tu nie ma breaka);
          // wtedy tez przy głębszym zagnieżdżeniu inaczej traktujemy zewnętrzne ciapki
          // (nizej ich nie moze byc ale jesli były na zewnątrz to moga byc spacje);
          // trzeba inaczej zapisywać ciapki bo przy wspólbieżności sie posypie
          break
        }

        else -> advanceError("Unexpected token in query value")
      }
    }

    if(queryQuotes != BazelqueryTokenTypes.WHITE_SPACE) {
      matchToken(queryQuotes)
      queryQuotes = BazelqueryTokenTypes.WHITE_SPACE
    }

    queryVal.done(BazelqueryElementTypes.QUERY_VAL)
  }


  // TODO: rozbic tak zeby zbierło odpowiednia ilosc argumentów (z typami -> dorobić INTEGER?)
  private fun parseCommand(queryQuotes: IElementType) {
    val command = mark()

    while (!atToken(BazelqueryTokenTypes.LEFT_PAREN) && !eof())
      advanceLexer()
    matchToken(BazelqueryTokenTypes.LEFT_PAREN)

    if (atToken(queryQuotes)) {
      command.done(BazelqueryElementTypes.COMMAND)
      return
    }

    if (atAnyToken(getAvailableWordsSet(queryQuotes))) parseWord()

    if (atToken(queryQuotes)) {
      command.done(BazelqueryElementTypes.COMMAND)
      return
    }

    while (!atToken(BazelqueryTokenTypes.RIGHT_PAREN) && !eof()) {

      if (atToken(queryQuotes)) {
        command.done(BazelqueryElementTypes.COMMAND)
        return
      }

      expectToken(BazelqueryTokenTypes.COMMA)

      if (atToken(queryQuotes)) {
        command.done(BazelqueryElementTypes.COMMAND)
        return
      }

      when {
        atAnyToken(getAvailableWordsSet(queryQuotes)) -> parseWord()
       // atToken(BazelqueryTokenTypes.INTEGER) -> advanceLexer()
       // atAnyToken(queryValPrefixes) -> parseQueryVal()
        else -> advanceError("<word> or <integer> expected")
      }

    }

    if (!matchToken(BazelqueryTokenTypes.RIGHT_PAREN)) {
      advanceError("<right parenthesis> expected")
    }

    command.done(BazelqueryElementTypes.COMMAND)
  }

  private fun parseWord() {
    val word = mark()
    advanceLexer()
    word.done(BazelqueryElementTypes.WORD)
  }


  private fun parseFlag() {
    val flag = mark()

    expectToken(BazelqueryTokenTypes.DOUBLE_HYPHEN)
    expectToken(BazelqueryTokenTypes.FLAG)
    expectToken(BazelqueryTokenTypes.EQUALS)
    expectToken(BazelqueryTokenTypes.VALUE)

    flag.done(BazelqueryTokenTypes.FLAG)
  }
}

