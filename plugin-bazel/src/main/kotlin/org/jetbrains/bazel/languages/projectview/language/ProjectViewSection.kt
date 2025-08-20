package org.jetbrains.bazel.languages.projectview.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider

interface SectionValueParser<T> {
  fun parse(value: String): T?
}

abstract class Instance<T>

abstract class ListInstance<T>(open val values: List<T>) : Instance<T>()

abstract class ScalarInstance<T>(open val value: T) : Instance<T>()

abstract class ProjectViewSection<T> {
  abstract val name: String
  abstract val doc: String?
  abstract val parser: SectionValueParser<T>
  abstract val completionProvider: CompletionProvider<CompletionParameters>?

  abstract fun buildInstance(rawValues: List<String>): Instance<T>?
}

abstract class ListSection<T> : ProjectViewSection<T>()

abstract class ScalarSection<T> : ProjectViewSection<T>()

sealed interface Value<T> {
  class Included<T>(val value: T) : Value<T>

  class Excluded<T>(val value: T) : Value<T>
}

fun isSectionSupported(sectionName: String): Boolean =
  BuiltInProjectViewSectionProvider.sections.any { it.name == sectionName }
