package org.jetbrains.bazel.languages.projectview.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.BAZEL_BINARY_KEY
import org.jetbrains.bazel.languages.projectview.ScalarSection
import java.nio.file.Path

internal class BazelBinarySection : ScalarSection<Path?>() {
  override val doc = "Specifies the path to a specific Bazel binary to be used in this project."

  override val sectionKey = BAZEL_BINARY_KEY

  override fun fromRawValue(rawValue: String): Path? =
    try {
      Path.of(rawValue)
    } catch (_: Exception) {
      null
    }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = annotatePath(element, holder)
}
