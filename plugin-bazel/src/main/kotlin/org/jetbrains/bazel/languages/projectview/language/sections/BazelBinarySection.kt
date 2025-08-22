package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey
import java.nio.file.Path
import kotlin.io.path.exists

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

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    var path = fromRawValue(element.text)
    if (path == null) {
      val message = ProjectViewBundle.getMessage("annotator.invalid.path.error")
      holder
        .newAnnotation(HighlightSeverity.ERROR, message)
        .range(element)
        .create()
      return
    }
    if (!path.isAbsolute) {
      val projectRoot = element.project.rootDir.toNioPath()
      path = projectRoot.resolve(path)
    }
    if (!path.exists()) {
      val message = ProjectViewBundle.getMessage("annotator.path.not.exists.error")
      holder
        .newAnnotation(HighlightSeverity.WARNING, message)
        .range(element)
        .create()
    }
  }

  companion object {
    const val NAME = "bazel_binary"
    val KEY = SectionKey<Path>(NAME)
  }
}
