package org.jetbrains.bazel.languages.projectview.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.ScalarSection
import org.jetbrains.bazel.languages.projectview.SectionKey
import java.nio.file.Path

class BazelBinarySection : ScalarSection<Path>() {
  override val name = NAME

  override val doc = "Specifies the path to a specific Bazel binary to be used in this project."

  override val sectionKey = KEY

  override fun fromRawValue(rawValue: String): Path? =
    try {
      Path.of(rawValue)
    } catch (_: Exception) {
      null
    }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = annotatePath(element, holder)

  companion object {
    const val NAME = "bazel_binary"
    val KEY = SectionKey<Path>(NAME)
  }
}
