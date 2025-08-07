package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType

abstract class StarlarkAnnotator : Annotator {
  protected fun checkElementAndParentType(
    element: PsiElement,
    expectedElementTypes: List<IElementType>,
    expectedParentTypes: List<IElementType>,
  ): Boolean = expectedElementTypes.contains(element.elementType) && expectedParentTypes.contains(element.parent.elementType)

  protected fun AnnotationHolder.mark(element: PsiElement, attr: TextAttributesKey) =
    newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(attr).create()

  protected fun AnnotationHolder.annotateError(
    element: PsiElement,
    message: String,
    fixList: List<IntentionAction> = emptyList(),
  ) {
    val annotationBuilder = newAnnotation(HighlightSeverity.ERROR, message).range(element)

    for (fix in fixList) {
      annotationBuilder.withFix(fix)
    }
    return annotationBuilder.create()
  }
}
