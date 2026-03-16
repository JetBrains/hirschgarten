package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType

/**
 * [DumbAware] because we don't build Starlark indexes anyway
 */
internal abstract class StarlarkAnnotator : Annotator, DumbAware {
  protected fun checkElementAndParentType(
    element: PsiElement,
    expectedElementTypes: List<IElementType>,
    expectedParentTypes: List<IElementType>,
  ): Boolean = expectedElementTypes.contains(element.elementType) && expectedParentTypes.contains(element.parent.elementType)

  protected fun AnnotationHolder.mark(element: PsiElement, attr: TextAttributesKey) =
    newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(attr).create()

  protected fun AnnotationHolder.annotateError(element: PsiElement, message: String) =
    newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
}
