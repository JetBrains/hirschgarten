package org.jetbrains.bazel.languages.projectview.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider

class ExcludableList<T>(val included: List<T>, val excluded: List<T>)

class TargetsSection : ListSection<ExcludableList<String>>() {
  override val name: String = NAME

  override fun getSectionKey(): SectionKey<ExcludableList<String>> = sectionKey

  override fun fromRawValues(rawValues: List<String>): ExcludableList<String> {
    val included = mutableListOf<String>()
    val excluded = mutableListOf<String>()
    for (rawValue in rawValues) {
      if (rawValue.startsWith("-")) {
        excluded.add(rawValue.substring(1))
      } else {
        included.add(rawValue)
      }
    }
    return ExcludableList(included, excluded)
  }

  override val doc = "List of targets to build"
  override val completionProvider = TargetCompletionProvider()

  companion object {
    const val NAME = "targets"
    val sectionKey = SectionKey<ExcludableList<String>>(NAME)
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

val REGISTERED_SECTIONS = listOf(TargetsSection())

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
}

private fun AnnotationHolder.annotateWarning(element: PsiElement, message: String) {
  newAnnotation(HighlightSeverity.WARNING, message).range(element).create()
}

private fun AnnotationHolder.annotateError(element: PsiElement, message: String) {
  newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
}
