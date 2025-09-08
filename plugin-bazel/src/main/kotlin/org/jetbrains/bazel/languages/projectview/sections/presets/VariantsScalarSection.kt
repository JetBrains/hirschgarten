package org.jetbrains.bazel.languages.projectview.sections.presets

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.ScalarSection

abstract class VariantsScalarSection<T>(private val variants: List<String>) : ScalarSection<T>() {
  final override val completionProvider = SimpleCompletionProvider(variants)

  final override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    val value = element.text
    if (value !in variants) {
      val message = ProjectViewBundle.getMessage("annotator.unknown.variant.error") + " " + variants.joinToString(", ")
      holder.annotateError(element, message)
    }
  }
}
