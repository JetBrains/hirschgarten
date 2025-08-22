package org.jetbrains.bazel.languages.projectview.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle

class SectionKey<T>(val name: String)

abstract class Section<T> {
  abstract val name: String

  open val default: T? = null

  abstract val sectionKey: SectionKey<T>

  abstract fun fromRawValues(rawValues: List<String>): T?

  abstract fun serialize(value: T): String

  open val doc: String? = null
  open val completionProvider: CompletionProvider<CompletionParameters>? = null

  open fun annotateValue(element: PsiElement, holder: AnnotationHolder) {}

  protected fun variantsAnnotation(
    element: PsiElement,
    holder: AnnotationHolder,
    variants: List<String>,
  ) {
    val value = element.text
    if (value !in variants) {
      val message = ProjectViewBundle.getMessage("annotator.unknown.variant.error") + " " + variants.joinToString(", ")
      holder.annotateError(element, message)
    }
  }

  protected fun AnnotationHolder.annotateError(element: PsiElement, message: String) =
    newAnnotation(HighlightSeverity.ERROR, message).range(element).create()

  protected fun AnnotationHolder.annotateWarning(element: PsiElement, message: String) =
    newAnnotation(HighlightSeverity.WARNING, message).range(element).create()
}

abstract class ScalarSection<T> : Section<T>() {
  abstract fun fromRawValue(rawValue: String): T?

  final override fun fromRawValues(rawValues: List<String>): T? {
    if (rawValues.size != 1) {
      return null
    }
    return fromRawValue(rawValues[0])
  }

  override fun serialize(value: T): String = "$name: $value"
}

abstract class ListSection<T : Collection<*>> : Section<T>() {
  override fun serialize(value: T): String = "$name:\n  ${value.joinToString(separator = "\n  ") { it.toString() }}"
}
