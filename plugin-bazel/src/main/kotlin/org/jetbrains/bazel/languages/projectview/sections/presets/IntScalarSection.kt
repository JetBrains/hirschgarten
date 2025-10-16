package org.jetbrains.bazel.languages.projectview.sections.presets

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.ScalarSection

abstract class IntScalarSection : ScalarSection<Int>() {
  final override fun fromRawValue(rawValue: String): Int? = rawValue.toIntOrNull()

  final override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    if (fromRawValue(element.text) == null) {
      val message = ProjectViewBundle.getMessage("annotator.cannot.parse.value", element.text)
      holder.annotateError(element, message)
    }
  }
}
