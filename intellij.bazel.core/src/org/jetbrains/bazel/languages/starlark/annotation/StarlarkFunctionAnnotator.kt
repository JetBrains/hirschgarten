package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.highlighting.starlarkSemanticHighlightingColor

internal class StarlarkFunctionAnnotator : Annotator, DumbAware {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val highlighting = element.starlarkSemanticHighlightingColor()
    if (highlighting != null) holder.annotateSilentInfo(element, highlighting)
  }
}
