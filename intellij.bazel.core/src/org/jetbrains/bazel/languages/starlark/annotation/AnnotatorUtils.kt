package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement


internal fun AnnotationHolder.annotateError(
  element: PsiElement,
  message: String
) = newAnnotation(HighlightSeverity.ERROR, message)
  .range(element)
  .create()

internal fun AnnotationHolder.annotateSilentInfo(
  element: PsiElement,
  attr: TextAttributesKey
) = newSilentAnnotation(HighlightSeverity.INFORMATION)
    .range(element)
    .textAttributes(attr)
    .create()
