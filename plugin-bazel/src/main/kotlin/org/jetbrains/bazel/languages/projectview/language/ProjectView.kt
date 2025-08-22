package org.jetbrains.bazel.languages.projectview.language

import org.jetbrains.bazel.languages.projectview.language.sections.AllowManualTargetsSyncSection
import org.jetbrains.bazel.languages.projectview.language.sections.BazelBinarySection
import org.jetbrains.bazel.languages.projectview.language.sections.BuildFlagsSection
import org.jetbrains.bazel.languages.projectview.language.sections.DebugFlagsSection
import org.jetbrains.bazel.languages.projectview.language.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.language.sections.SyncFlagsSection
import org.jetbrains.bazel.languages.projectview.language.sections.TargetsSection
import org.jetbrains.bazel.languages.projectview.language.sections.TestFlagsSection
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

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

val REGISTERED_SECTIONS =
  listOf(
    TargetsSection(),
    ShardSyncSection(),
    BazelBinarySection(),
    BuildFlagsSection(),
    SyncFlagsSection(),
    TestFlagsSection(),
    DebugFlagsSection(),
    AllowManualTargetsSyncSection(),
  )

fun getSectionByName(name: String): Section<*>? = REGISTERED_SECTIONS.find { it.name == name }

inline fun <reified T> getSectionByType(): T? = REGISTERED_SECTIONS.find { it is T } as T

class ProjectView(rawSections: List<Pair<String, List<String>>>) {
  val sections = mutableMapOf<SectionKey<*>, Any>()

  init {
    for ((name, values) in rawSections) {
      val section = getSectionByName(name)
      val parsed = section?.fromRawValues(values) ?: continue
      sections[section.sectionKey] = parsed
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
