package org.jetbrains.bazel.languages.projectview.sections.presets

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.ScalarSection
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider

@ApiStatus.Internal
abstract class VariantsScalarSection<T>(private val variants: List<String>) : ScalarSection<T>() {
  final override val completionProvider: CompletionProvider<CompletionParameters> = SimpleCompletionProvider(variants)

  final override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    val value = element.text
    if (value !in variants) {
      val message = ProjectViewBundle.getMessage("annotator.unknown.variant.error") + " " + variants.joinToString(", ")
      holder.annotateError(element, message)
    }
  }
}
