package org.jetbrains.bazel.languages.projectview

import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
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
import org.jetbrains.bazel.languages.projectview.sections.PythonCodeGeneratorRuleNamesSection
import org.jetbrains.bazel.languages.projectview.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.sections.ShardingApproachSection
import org.jetbrains.bazel.languages.projectview.sections.SyncFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.TargetShardSizeSection
import org.jetbrains.bazel.languages.projectview.sections.TargetsSection
import org.jetbrains.bazel.languages.projectview.sections.TestFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.UseJetBrainsTestRunnerSection
import java.nio.file.Path

// Extension properties to provide convenient access to ProjectView sections
// These match the properties that were available in WorkspaceContext

val ProjectView.targets: List<ExcludableValue<Label>>
  get() = getSection(TargetsSection.KEY) ?: emptyList()

val ProjectView.directories: List<ExcludableValue<Path>>
  get() = getSection(DirectoriesSection.KEY) ?: emptyList()

val ProjectView.buildFlags: List<String>
  get() = getSection(BuildFlagsSection.KEY)?.map { it.toString() } ?: emptyList()

val ProjectView.syncFlags: List<String>
  get() = getSection(SyncFlagsSection.KEY)?.map { it.toString() } ?: emptyList()

val ProjectView.debugFlags: List<String>
  get() = getSection(DebugFlagsSection.KEY)?.map { it.toString() } ?: emptyList()

val ProjectView.testFlags: List<String>
  get() = getSection(TestFlagsSection.KEY)?.map { it.toString() } ?: emptyList()

val ProjectView.bazelBinary: Path?
  get() = getSection(BazelBinarySection.KEY)

val ProjectView.allowManualTargetsSync: Boolean
  get() = getSection(AllowManualTargetsSyncSection.KEY) ?: false

val ProjectView.deriveTargetsFromDirectories: Boolean
  get() = getSection(DeriveTargetsFromDirectoriesSection.KEY) ?: false

val ProjectView.importDepth: Int
  get() = getSection(ImportDepthSection.KEY) ?: 1

val ProjectView.enabledRules: List<String>
  get() = getSection(EnabledRulesSection.KEY) ?: emptyList()

val ProjectView.ideJavaHomeOverride: Path?
  get() = getSection(IdeJavaHomeOverrideSection.KEY)

val ProjectView.enableNativeAndroidRules: Boolean
  get() = getSection(EnableNativeAndroidRulesSection.KEY) ?: false

val ProjectView.androidMinSdk: Int?
  get() = getSection(AndroidMinSdkSection.KEY)

val ProjectView.shardSync: Boolean
  get() = getSection(ShardSyncSection.KEY) ?: false

val ProjectView.targetShardSize: Int
  get() = getSection(TargetShardSizeSection.KEY) ?: 1000

val ProjectView.shardingApproach: String?
  get() = getSection(ShardingApproachSection.KEY)?.name?.lowercase()

val ProjectView.importRunConfigurations: List<String>
  get() = getSection(ImportRunConfigurationsSection.KEY)?.map { it.toString() } ?: emptyList()

val ProjectView.gazelleTarget: Label?
  get() = getSection(GazelleTargetSection.KEY)

val ProjectView.indexAllFilesInDirectories: Boolean
  get() = getSection(IndexAllFilesInDirectoriesSection.KEY) ?: false

val ProjectView.pythonCodeGeneratorRuleNames: List<String>
  get() = getSection(PythonCodeGeneratorRuleNamesSection.KEY) ?: emptyList()

val ProjectView.importIjars: Boolean
  get() = getSection(ImportIjarsSection.KEY) ?: false

val ProjectView.deriveInstrumentationFilterFromTargets: Boolean
  get() = getSection(DeriveInstrumentationFilterFromTargetsSection.KEY) ?: false

val ProjectView.indexAdditionalFilesInDirectories: List<String>
  get() = getSection(IndexAdditionalFilesInDirectoriesSection.KEY) ?: emptyList()

val ProjectView.useJetBrainsTestRunner: Boolean
  get() = getSection(UseJetBrainsTestRunnerSection.KEY) ?: false

/**
 * List of names of repositories that should be treated as internal because there are some targets that we want to be imported that
 * belong to them.
 */
val ProjectView.externalRepositoriesTreatedAsInternal: List<String>
  get() =
    targets
      .mapNotNull { excludableValue ->
        excludableValue.value
          .assumeResolved()
          .repo.repoName
          .takeIf { repoName -> repoName.isNotEmpty() }
      }.distinct()
