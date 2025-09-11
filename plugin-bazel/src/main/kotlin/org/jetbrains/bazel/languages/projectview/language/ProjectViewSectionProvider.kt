package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.extensions.ExtensionPointName
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
import org.jetbrains.bazel.languages.projectview.language.sections.IndexAdditionalFilesInDirectoriesSection
import org.jetbrains.bazel.languages.projectview.language.sections.IndexAllFilesInDirectoriesSection
import org.jetbrains.bazel.languages.projectview.language.sections.PythonCodeGeneratorRuleNamesSection
import org.jetbrains.bazel.languages.projectview.language.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.language.sections.ShardingApproachSection
import org.jetbrains.bazel.languages.projectview.language.sections.SyncFlagsSection
import org.jetbrains.bazel.languages.projectview.language.sections.TargetShardSizeSection
import org.jetbrains.bazel.languages.projectview.language.sections.TargetsSection
import org.jetbrains.bazel.languages.projectview.language.sections.TestFlagsSection

interface ProjectViewSectionProvider {
  val sections: List<Section<*>>

  companion object {
    val EP = ExtensionPointName<ProjectViewSectionProvider>("org.jetbrains.bazel.projectViewSectionProvider")
  }
}

class DefaultProjectViewSectionProvider : ProjectViewSectionProvider {
  override val sections =
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
      IndexAdditionalFilesInDirectoriesSection(),
    )
}

object ProjectViewSections {
  val REGISTERED_SECTIONS = ProjectViewSectionProvider.EP.extensionList.flatMap { it.sections }

  fun getSectionByName(name: String): Section<*>? = REGISTERED_SECTIONS.find { it.name == name }

  inline fun <reified T> getSectionByType(): T? = REGISTERED_SECTIONS.find { it is T } as T
}
