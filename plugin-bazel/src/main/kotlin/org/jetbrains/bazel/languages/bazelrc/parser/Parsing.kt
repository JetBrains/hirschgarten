package org.jetbrains.bazel.languages.bazelrc.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcElementTypes
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenSets
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

open class Parsing(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {
  companion object {
    private val commandPrefixes = TokenSet.orSet(BazelrcTokenSets.COMMANDS, BazelrcTokenSets.QUOTES)

    @JvmStatic
    protected fun advanceError(builder: PsiBuilder, message: String) {
      val err = builder.mark()
      builder.advanceLexer()
      err.error(message)
    }
  }

  fun parseFile(): ASTNode {
    val file = builder.mark()
    while (!eof()) {
      parseLine()
    }

    file.done(root)
    return treeBuilt
  }

  private fun parseLine() {
    atAnyOfTokens(commandPrefixes).ifFalse {
      advanceError(builder, "New command line expected")
    }

    val lineMarker = mark()
    parseCommand()
    while (!eof() && atToken(BazelrcTokenTypes.FLAG)) {
      parseFlag()
    }
    lineMarker.done(BazelrcElementTypes.LINE)
  }

  private fun parseCommand() {
    var quoteToken: IElementType? = null
    if (atAnyOfTokens(BazelrcTokenSets.QUOTES)) {
      quoteToken = tokenType
      advanceLexer()
    }

    matchAnyToken(BazelrcTokenSets.COMMANDS)
    matchToken(BazelrcTokenTypes.COLON).ifTrue {
      matchToken(BazelrcTokenTypes.CONFIG)
    }

    if (quoteToken != null) {
      matchToken(quoteToken)
    }
  }

  private fun parseFlag() {
    advanceLexer()
  }

  private fun atToken(tokenType: IElementType?): Boolean = builder.tokenType === tokenType

  private fun atAnyOfTokens(tokenTypes: TokenSet): Boolean = tokenTypes.contains(tokenType)

  private fun matchToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      advanceLexer()
      return true
    }
    return false
  }

  private fun matchAnyToken(tokens: TokenSet): Boolean {
    if (atAnyOfTokens(tokens)) {
      advanceLexer()
      return true
    }
    return false
  }
}
