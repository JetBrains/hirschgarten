package org.jetbrains.bazel.languages.starlark.matching

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

class StarlarkBraceMatcher : PairedBraceMatcher {
  override fun getPairs(): Array<BracePair> = arrayOf(
    BracePair(StarlarkTokenTypes.LPAR, StarlarkTokenTypes.RPAR, false),
    BracePair(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.RBRACKET, false),
    BracePair(StarlarkTokenTypes.LBRACE, StarlarkTokenTypes.RBRACE, false),
  )

  override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean =
    contextType in StarlarkTokenSets.WHITESPACE ||
      contextType == StarlarkTokenTypes.COMMENT ||
      contextType == StarlarkTokenTypes.COLON ||
      contextType == StarlarkTokenTypes.COMMA ||
      contextType == StarlarkTokenTypes.RPAR ||
      contextType == StarlarkTokenTypes.RBRACKET ||
      contextType == StarlarkTokenTypes.RBRACE ||
      contextType == StarlarkTokenTypes.LBRACE ||
      contextType == null

  override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
