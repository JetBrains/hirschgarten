package org.jetbrains.bazel.languages.projectview.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

sealed class ExcludableValue<T> {
  abstract val value: T?

  data class Included<T>(override val value: T) : ExcludableValue<T>()

  data class Excluded<T>(override val value: T) : ExcludableValue<T>()

  fun isIncluded(): Boolean = this is Included

  fun isExcluded(): Boolean = this is Excluded

  companion object {
    fun <T> included(value: T): ExcludableValue<T> = Included(value)

    fun <T> excluded(value: T): ExcludableValue<T> = Excluded(value)
  }
}

class TargetsSection : ListSection<List<ExcludableValue<String>>>() {
  override val name: String = NAME

  override fun getSectionKey(): SectionKey<List<ExcludableValue<String>>> = sectionKey

  override fun fromRawValues(rawValues: List<String>): List<ExcludableValue<String>> =
    rawValues.map {
      if (it.startsWith("-")) {
        ExcludableValue.excluded(it.substring(1))
      } else {
        ExcludableValue.included(it)
      }
    }

  override val doc = "List of targets to build"
  override val completionProvider = TargetCompletionProvider()

  companion object {
    const val NAME = "targets"
    val sectionKey = SectionKey<List<ExcludableValue<String>>>(NAME)
  }
}

class ShardSyncSection : Section<Boolean>() {
  override val name: String = NAME

  override fun getSectionKey(): SectionKey<Boolean> = sectionKey

  override fun fromRawValues(rawValues: List<String>): Boolean? {
    if (rawValues.size != 1) {
      return null
    }
    return rawValues[0].toBooleanStrictOrNull()
  }

  companion object {
    const val NAME = "shard_sync"
    val sectionKey = SectionKey<Boolean>(NAME)
  }
}

val REGISTERED_SECTIONS = listOf(TargetsSection(), ShardSyncSection())

fun getSectionByName(name: String): Section<*>? = REGISTERED_SECTIONS.find { it.name == name }

class SectionKey<T>(val name: String)

class ProjectView(rawSections: List<Pair<String, List<String>>>) {
  val sections = mutableMapOf<SectionKey<*>, Any>()

  init {
    for ((name, values) in rawSections) {
      val section = getSectionByName(name)
      val parsed = section?.fromRawValues(values) ?: continue
      sections[section.getSectionKey()] = parsed
    }
  }

  inline fun <reified T> getSection(key: SectionKey<T>): T? = sections[key] as T

  companion object {
    fun fromProjectViewPsiFile(file: ProjectViewPsiFile): ProjectView {
      val rawSections = mutableListOf<Pair<String, List<String>>>()
      val psiSections = file.getSections()
      for (section in psiSections) {
        val name = section.getKeyword().text.trim()
        val values = section.getItems().map { it.text.trim() }
        rawSections.add(Pair(name, values))
      }
      return ProjectView(rawSections)
    }
  }
}

private fun AnnotationHolder.annotateWarning(element: PsiElement, message: String) {
  newAnnotation(HighlightSeverity.WARNING, message).range(element).create()
}

private fun AnnotationHolder.annotateError(element: PsiElement, message: String) {
  newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
}
