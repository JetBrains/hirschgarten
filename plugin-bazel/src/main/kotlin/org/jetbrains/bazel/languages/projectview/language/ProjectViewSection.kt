package org.jetbrains.bazel.languages.projectview.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement

interface SectionValueParser<T> {
  fun parse(value: String): T?
}

abstract class Section<T> {
  abstract val name: String

  abstract fun fromRawValues(rawValues: List<String>): T?

  abstract fun getSectionKey(): SectionKey<T>

  open val doc: String? = null
  open val completionProvider: CompletionProvider<CompletionParameters>? = null

  open fun annotateValue(element: PsiElement, holder: AnnotationHolder) {}
}

abstract class ListSection<T> : Section<T>()

abstract class ScalarSection<T> : Section<T>()
