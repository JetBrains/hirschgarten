package org.jetbrains.bazel.languages.projectview.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.IDE_JAVA_HOME_OVERRIDE_KEY
import org.jetbrains.bazel.languages.projectview.ScalarSection
import java.nio.file.Path

internal class IdeJavaHomeOverrideSection : ScalarSection<Path?>() {
  override val sectionKey = IDE_JAVA_HOME_OVERRIDE_KEY
  override val doc = "Local java home path to override to use with IDE, e.g. IntelliJ IDEA"

  override fun fromRawValue(rawValue: String): Path? =
    try {
      Path.of(rawValue)
    } catch (_: Exception) {
      null
    }

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = annotatePath(element, holder)
}
