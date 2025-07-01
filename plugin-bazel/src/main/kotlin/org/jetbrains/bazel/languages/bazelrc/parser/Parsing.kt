package org.jetbrains.bazel.languages.bazelrc.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcElementTypes
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenSets
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes

open class Parsing(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {
  companion object {
    private val commandPrefixes = TokenSet.create(BazelrcTokenTypes.COMMAND, *BazelrcTokenSets.QUOTES.types)
    private val flagPrefixes = TokenSet.create(BazelrcTokenTypes.FLAG, BazelrcTokenTypes.EQ, BazelrcTokenTypes.VALUE)
    private val valueTokens = TokenSet.create(BazelrcTokenTypes.VALUE, BazelrcTokenTypes.SINGLE_QUOTE, BazelrcTokenTypes.DOUBLE_QUOTE)
    private val backTokens = TokenSet.create(TokenType.WHITE_SPACE, BazelrcTokenTypes.COMMENT)
  }

  fun parseFile(): ASTNode {
    val file = builder.mark()
    while (!eof()) {
      when {
        atToken(BazelrcTokenTypes.IMPORT) -> parseImport()

        atAnyToken(commandPrefixes) -> parseLine()

        else -> advanceError("<import>, <try-import> or command expected)")
      }
    }

    file.done(root)
    return treeBuilt
  }

  private fun parseImport() {
    val import = mark()

    expectToken(BazelrcTokenTypes.IMPORT, "<import> or <try-import> expected")

    if (!matchToken(BazelrcTokenTypes.VALUE)) {
      error(BazelPluginBundle.message("bazelrc.language.parser.missing.import.path"))
    }

    while (atToken(BazelrcTokenTypes.VALUE)) {
      advanceError("Unexpected import path")
    }

    import.done(BazelrcElementTypes.IMPORT)
  }

  private fun parseLine() {
    if (!atAnyToken(commandPrefixes)) {
      advanceError("New command line expected")
    }

    val lineMarker = mark()
    parseCommand()

    while (atAnyToken(flagPrefixes)) {
      parseFlag()
    }

    lineMarker.done(BazelrcElementTypes.LINE)
  }

  private fun parseCommand() {
    var quote: IElementType? = null
    if (atAnyToken(BazelrcTokenSets.QUOTES)) {
      quote = tokenType
      advanceLexer()
    }

    expectToken(BazelrcTokenTypes.COMMAND, "Command expected")

    if (matchToken(BazelrcTokenTypes.COLON)) {
      matchToken(BazelrcTokenTypes.CONFIG)
    }

    // check to see if we crossed a nl
    if (quote != null && !pastNewLine()) {
      matchToken(quote)
    }
  }

  private fun parseFlag() {
    val flag = mark()

    matchToken(BazelrcTokenTypes.FLAG)
    matchToken(BazelrcTokenTypes.EQ)
    matchToken(BazelrcTokenTypes.VALUE)
    while (atAnyToken(valueTokens) && !pastNewLine()) {
      advanceLexer()
    }

    flag.done(BazelrcElementTypes.FLAG)
  }

  private fun pastNewLine(): Boolean {
    var tokenPos = -1
    var endOffset = currentOffset

    var backToken = rawLookup(tokenPos)
    while (backTokens.contains(backToken)) {
      val startOffset = rawTokenTypeStart(tokenPos)
      if (originalText.substring(startOffset, endOffset).contains('\n')) {
        return true
      }
      endOffset = startOffset
      backToken = rawLookup(--tokenPos)
    }

    return false
  }

  private fun atToken(tokenType: IElementType?): Boolean = builder.tokenType === tokenType

  private fun atAnyToken(tokenTypes: TokenSet): Boolean = tokenTypes.contains(tokenType)

  private fun matchToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      advanceLexer()
      return true
    }
    return false
  }

  private fun expectToken(tokenType: IElementType, error: String): Boolean {
    if (atToken(tokenType)) {
      advanceLexer()
      return true
    } else {
      advanceError(error)
      return false
    }
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
}
