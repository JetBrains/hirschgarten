package org.jetbrains.bazel.languages.projectview.language

import org.jetbrains.bazel.languages.projectview.language.sections.AllowManualTargetsSyncSection
import org.jetbrains.bazel.languages.projectview.language.sections.AndroidMinSdkSection
import org.jetbrains.bazel.languages.projectview.language.sections.BazelBinarySection
import org.jetbrains.bazel.languages.projectview.language.sections.BuildFlagsSection
import org.jetbrains.bazel.languages.projectview.language.sections.DebugFlagsSection
import org.jetbrains.bazel.languages.projectview.language.sections.DeriveInstrumentationFilterFromTargetsSection
import org.jetbrains.bazel.languages.projectview.language.sections.DeriveTargetsFromDirectoriesSection
import org.jetbrains.bazel.languages.projectview.language.sections.DirectoriesSection
import org.jetbrains.bazel.languages.projectview.language.sections.EnableNativeAndroidRulesSection
import org.jetbrains.bazel.languages.projectview.language.sections.EnabledRulesSection
import org.jetbrains.bazel.languages.projectview.language.sections.GazelleTargetSection
import org.jetbrains.bazel.languages.projectview.language.sections.IdeJavaHomeOverrideSection
import org.jetbrains.bazel.languages.projectview.language.sections.ImportDepthSection
import org.jetbrains.bazel.languages.projectview.language.sections.ImportIjarsSection
import org.jetbrains.bazel.languages.projectview.language.sections.ImportRunConfigurationsSection
import org.jetbrains.bazel.languages.projectview.language.sections.IndexAllFilesInDirectoriesSection
import org.jetbrains.bazel.languages.projectview.language.sections.PythonCodeGeneratorRuleNamesSection
import org.jetbrains.bazel.languages.projectview.language.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.language.sections.ShardingApproachSection
import org.jetbrains.bazel.languages.projectview.language.sections.SyncFlagsSection
import org.jetbrains.bazel.languages.projectview.language.sections.TargetShardSizeSection
import org.jetbrains.bazel.languages.projectview.language.sections.TargetsSection
import org.jetbrains.bazel.languages.projectview.language.sections.TestFlagsSection
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

val REGISTERED_SECTIONS =
  listOf(
    AllowManualTargetsSyncSection(),
    AndroidMinSdkSection(),
    BazelBinarySection(),
    BuildFlagsSection(),
    DebugFlagsSection(),
    DeriveInstrumentationFilterFromTargetsSection(),
    DeriveTargetsFromDirectoriesSection(),
    DirectoriesSection(),
    EnabledRulesSection(),
    EnableNativeAndroidRulesSection(),
    GazelleTargetSection(),
    IdeJavaHomeOverrideSection(),
    ImportDepthSection(),
    ImportIjarsSection(),
    ImportRunConfigurationsSection(),
    IndexAllFilesInDirectoriesSection(),
    PythonCodeGeneratorRuleNamesSection(),
    ShardingApproachSection(),
    ShardSyncSection(),
    SyncFlagsSection(),
    TargetShardSizeSection(),
    TargetsSection(),
    TestFlagsSection(),
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
