package org.jetbrains.bazel.languages.projectview

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.rootDir
import java.nio.file.Path
import kotlin.io.path.exists

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

  companion object {
    fun annotatePath(element: PsiElement, holder: AnnotationHolder) {
      val pathString = element.text
      try {
        var path = Path.of(pathString)
        if (!path.isAbsolute) {
          val projectRoot = element.project.rootDir.toNioPath()
          path = projectRoot.resolve(path)
        }
        if (!path.exists()) {
          val message = ProjectViewBundle.getMessage("annotator.path.not.exists.error")
          holder.annotateWarning(element, message)
        }
      } catch (e: Exception) {
        val message = ProjectViewBundle.getMessage("annotator.invalid.path.error")
        holder.annotateError(element, message)
      }
    }

    fun AnnotationHolder.annotateError(element: PsiElement, message: String) =
      newAnnotation(HighlightSeverity.ERROR, message).range(element).create()

    fun AnnotationHolder.annotateWarning(element: PsiElement, message: String) =
      newAnnotation(HighlightSeverity.WARNING, message).range(element).create()
  }
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
