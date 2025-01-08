package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors
import org.jetbrains.bazel.languages.starlark.references.StarlarkNamedArgumentReference

class StarlarkFunctionAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when {
      isFunctionDeclaration(element) -> holder.mark(element, StarlarkHighlightingColors.FUNCTION_DECLARATION)
      isNamedArgument(element) -> annotateNamedArgument(element, holder)
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

  private fun annotateNamedArgument(element: PsiElement, holder: AnnotationHolder) {
    if (isNotResolvedNamedArgument(element)) {
      if (element.elementType == StarlarkTokenTypes.IDENTIFIER) {
        holder.annotateError(element, StarlarkBundle.message("annotator.named.parameter.not.found", element.text))
      }
    } else {
      holder.mark(element, StarlarkHighlightingColors.NAMED_ARGUMENT)
    }
  }

  private fun isNotResolvedNamedArgument(element: PsiElement): Boolean {
    val reference = (element.parent.reference as? StarlarkNamedArgumentReference) ?: return false
    return reference.resolveFunction() != null && reference.resolve() == null
  }
}
