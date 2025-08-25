package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey
import java.nio.file.Path

class IdeJavaHomeOverrideSection : ScalarSection<Path>() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Local java home path to override to use with IDE, e.g. IntelliJ IDEA"

  override fun fromRawValue(rawValue: String): Path? =
    try {
      Path.of(rawValue)
    } catch (_: Exception) {
      null
    }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = annotatePath(element, holder)

  companion object {
    const val NAME = "ide_java_home_override"
    val KEY = SectionKey<Path>(NAME)
  }
}
