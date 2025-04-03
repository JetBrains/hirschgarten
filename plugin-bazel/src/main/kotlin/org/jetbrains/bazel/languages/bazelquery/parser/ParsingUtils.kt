package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class ParsingUtils(private val builder: PsiBuilder) {
  fun atToken(tokenType: IElementType?): Boolean = builder.tokenType === tokenType

  fun atAnyToken(tokenTypes: TokenSet): Boolean = tokenTypes.contains(builder.tokenType)

  fun matchToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      builder.advanceLexer()
      return true
    }
    return false
  }

  fun expectToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      builder.advanceLexer()
      return true
    }
    advanceError("<$tokenType> expected")
    return false
  }

  fun matchAnyToken(tokens: TokenSet): Boolean {
    if (atAnyToken(tokens)) {
      builder.advanceLexer()
      return true
    }
    return false
  }

  fun advanceError(message: String) {
    val err = builder.mark()
    builder.advanceLexer()
    err.error(message)
  }
}
