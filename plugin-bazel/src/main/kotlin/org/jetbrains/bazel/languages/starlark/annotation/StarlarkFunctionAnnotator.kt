package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

class StarlarkFunctionAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when {
      isFunctionDeclaration(element) -> holder.mark(element, StarlarkHighlightingColors.FUNCTION_DECLARATION)
      isNamedArgument(element) -> holder.mark(element, StarlarkHighlightingColors.NAMED_ARGUMENT)
      else -> {}
    }
  }

  private fun isFunctionDeclaration(element: PsiElement): Boolean = checkElementAndParentType(
    element = element,
    expectedElementTypes = listOf(StarlarkTokenTypes.IDENTIFIER),
    expectedParentType = listOf(StarlarkElementTypes.FUNCTION_DECLARATION),
  )

  private fun isNamedArgument(element: PsiElement): Boolean = checkElementAndParentType(
    element = element,
    expectedElementTypes = listOf(StarlarkTokenTypes.IDENTIFIER, StarlarkTokenTypes.EQ),
    expectedParentType = listOf(StarlarkElementTypes.NAMED_ARGUMENT_EXPRESSION),
  )

  private fun checkElementAndParentType(
    element: PsiElement,
    expectedElementTypes: List<IElementType>,
    expectedParentType: List<IElementType>,
  ): Boolean =
    expectedElementTypes.contains(element.elementType) && expectedParentType.contains(element.parent.elementType)
}

private fun AnnotationHolder.mark(element: PsiElement, attr: TextAttributesKey) {
  newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(attr).create()
}
