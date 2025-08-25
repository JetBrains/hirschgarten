package org.jetbrains.bazel.languages.projectview.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.commands
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.language.sections.SyncFlagsSection.Companion.COMMAND
import java.nio.file.Path
import kotlin.io.path.exists

sealed class ExcludableValue<T> {
  abstract val value: T

  data class Included<T>(override val value: T) : ExcludableValue<T>() {
    override fun toString(): String = value.toString()
  }

  data class Excluded<T>(override val value: T) : ExcludableValue<T>() {
    override fun toString(): String = "-$value"
  }

  fun isIncluded(): Boolean = this is Included

  fun isExcluded(): Boolean = this is Excluded

  companion object {
    fun <T> included(value: T): ExcludableValue<T> = Included(value)

    fun <T> excluded(value: T): ExcludableValue<T> = Excluded(value)
  }
}

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
    protected fun variantsAnnotation(
      element: PsiElement,
      holder: AnnotationHolder,
      variants: List<String>,
    ) {
      val value = element.text
      if (value !in variants) {
        val message = ProjectViewBundle.getMessage("annotator.unknown.variant.error") + " " + variants.joinToString(", ")
        holder.annotateError(element, message)
      }
    }

    fun annotateFlag(
      element: PsiElement,
      holder: AnnotationHolder,
      command: String,
    ) {
      val flag = Flag.byName(element.text)
      if (flag == null) {
        val message = ProjectViewBundle.getMessage("annotator.unknown.flag.error", element.text)
        holder.annotateError(element, message)
        return
      }
      if (command !in flag.commands()) {
        val message = ProjectViewBundle.getMessage("annotator.flag.not.allowed.here.error", element.text, COMMAND)
        holder.annotateError(element, message)
      }
    }

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
