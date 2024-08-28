package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors

class StarlarkFunctionAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when {
      isFunctionDeclaration(element) -> holder.mark(element, StarlarkHighlightingColors.FUNCTION_DECLARATION)
      isNamedArgument(element) -> holder.mark(element, StarlarkHighlightingColors.NAMED_ARGUMENT)
      else -> {}
    }
  }

  private fun isFunctionDeclaration(element: PsiElement): Boolean =
    checkElementAndParentType(
      element = element,
      expectedElementTypes = listOf(StarlarkTokenTypes.IDENTIFIER),
      expectedParentTypes = listOf(StarlarkElementTypes.FUNCTION_DECLARATION),
    )

  private fun isNamedArgument(element: PsiElement): Boolean =
    checkElementAndParentType(
      element = element,
      expectedElementTypes = listOf(StarlarkTokenTypes.IDENTIFIER, StarlarkTokenTypes.EQ),
      expectedParentTypes = listOf(StarlarkElementTypes.NAMED_ARGUMENT_EXPRESSION),
    )
}
