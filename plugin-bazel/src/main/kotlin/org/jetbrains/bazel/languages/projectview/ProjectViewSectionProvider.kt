package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.languages.projectview.language.sections.IndexAdditionalFilesInDirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.AllowManualTargetsSyncSection
import org.jetbrains.bazel.languages.projectview.sections.AndroidMinSdkSection
import org.jetbrains.bazel.languages.projectview.sections.BazelBinarySection
import org.jetbrains.bazel.languages.projectview.sections.BuildFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.DebugFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.DeriveInstrumentationFilterFromTargetsSection
import org.jetbrains.bazel.languages.projectview.sections.DeriveTargetsFromDirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.DirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.EnableNativeAndroidRulesSection
import org.jetbrains.bazel.languages.projectview.sections.EnabledRulesSection
import org.jetbrains.bazel.languages.projectview.sections.GazelleTargetSection
import org.jetbrains.bazel.languages.projectview.sections.IdeJavaHomeOverrideSection
import org.jetbrains.bazel.languages.projectview.sections.ImportDepthSection
import org.jetbrains.bazel.languages.projectview.sections.ImportIjarsSection
import org.jetbrains.bazel.languages.projectview.sections.ImportRunConfigurationsSection
import org.jetbrains.bazel.languages.projectview.sections.IndexAllFilesInDirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.PreferClassJarsOverSourcelessJarsSection
import org.jetbrains.bazel.languages.projectview.sections.PythonCodeGeneratorRuleNamesSection
import org.jetbrains.bazel.languages.projectview.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.sections.ShardingApproachSection
import org.jetbrains.bazel.languages.projectview.sections.SyncFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.TargetShardSizeSection
import org.jetbrains.bazel.languages.projectview.sections.TargetsSection
import org.jetbrains.bazel.languages.projectview.sections.TestFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.UseJetBrainsTestRunnerSection

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
      UseJetBrainsTestRunnerSection(),
      PreferClassJarsOverSourcelessJarsSection()
    )
}

object ProjectViewSections {
  val REGISTERED_SECTIONS get() = ProjectViewSectionProvider.EP.extensionList.flatMap { it.sections }

  fun getSectionByName(name: String): Section<*>? = REGISTERED_SECTIONS.find { it.name == name }

  inline fun <reified T> getSectionByType(): T? = REGISTERED_SECTIONS.find { it is T } as T
}
