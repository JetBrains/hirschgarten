package org.jetbrains.bsp.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsContexts.ParsingError
import com.intellij.psi.tree.IElementType
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bsp.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bsp.bazel.languages.starlark.elements.StarlarkTokenType
import org.jetbrains.bsp.bazel.languages.starlark.elements.StarlarkTokenTypes

open class Parsing(val context: ParsingContext) {
  protected val builder = context.builder

  protected fun checkMatches(token: IElementType, message: @ParsingError String): Boolean {
    if (atToken(token)) {
      builder.advanceLexer()
      return true
    }
    builder.error(message)
    return false
  }

  protected fun parseIdentifierOrSkip(): Boolean {
    return if (atToken(StarlarkTokenTypes.IDENTIFIER)) {
      builder.advanceLexer()
      true
    } else {
      val nameExpected = builder.mark()
      if (!atToken(StarlarkTokenTypes.STATEMENT_BREAK) && !atToken(StarlarkTokenTypes.LPAR)) {
        builder.advanceLexer()
      }
      nameExpected.error(StarlarkBundle.message("parser.expected.identifier"))
      false
    }
  }

  protected fun assertCurrentToken(tokenType: StarlarkTokenType) {
    LOG.assertTrue(atToken(tokenType))
  }

  protected fun atToken(tokenType: IElementType?): Boolean = builder.tokenType === tokenType

  protected fun atAnyOfTokens(tokenTypes: List<IElementType?>): Boolean {
    val currentTokenType = builder.tokenType
    for (tokenType in tokenTypes) {
      if (currentTokenType === tokenType) return true
    }
    return false
  }

  protected fun matchToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      builder.advanceLexer()
      return true
    }
    return false
  }

  protected fun nextToken() {
    builder.advanceLexer()
  }

  companion object {
    private val LOG = Logger.getInstance(Parsing::class.java)

    @JvmStatic
    protected fun advanceIdentifierLike(builder: PsiBuilder) {
      if (isReservedKeyword(builder)) {
        val tokenText = builder.tokenText ?: ""
        advanceError(builder, StarlarkBundle.message("parser.reserved.keyword.cannot.be.used.as.identifier", tokenText))
      } else {
        builder.advanceLexer()
      }
    }

    @JvmStatic
    protected fun advanceError(builder: PsiBuilder, message: @ParsingError String) {
      val err = builder.mark()
      builder.advanceLexer()
      err.error(message)
    }

    @JvmStatic
    protected fun isIdentifierLike(builder: PsiBuilder): Boolean =
      builder.tokenType === StarlarkTokenTypes.IDENTIFIER || isReservedKeyword(builder)

    private fun isReservedKeyword(builder: PsiBuilder): Boolean =
      StarlarkTokenSets.RESERVED_KEYWORDS.contains(builder.tokenType)

    @JvmStatic
    protected fun buildTokenElement(type: IElementType?, builder: PsiBuilder) {
      val marker = builder.mark()
      advanceIdentifierLike(builder)
      marker.done(type!!)
    }
  }
}
