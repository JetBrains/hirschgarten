package org.jetbrains.bsp.bazel.languages.starlark.elements

import com.intellij.psi.tree.TokenSet

object StarlarkTokenSets {
  val WHITESPACE = TokenSet.create(StarlarkTokenTypes.SPACE, StarlarkTokenTypes.TAB, StarlarkTokenTypes.LINE_BREAK)

  val COMMENT = TokenSet.create(StarlarkTokenTypes.COMMENT)

  val STRINGS = TokenSet.create(StarlarkTokenTypes.STRING, StarlarkTokenTypes.BYTES)

  val OPEN_BRACKETS = TokenSet.create(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.LBRACE, StarlarkTokenTypes.LPAR)

  val CLOSE_BRACKETS = TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.RBRACE, StarlarkTokenTypes.RPAR)
}
