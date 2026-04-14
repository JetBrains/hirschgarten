package org.jetbrains.bazel.languages.projectview

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.language.sections.IndexAdditionalFilesInDirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.AllowManualTargetsSyncSection
import org.jetbrains.bazel.languages.projectview.sections.BazelBinarySection
import org.jetbrains.bazel.languages.projectview.sections.BuildFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.DebugFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.DeriveInstrumentationFilterFromTargetsSection
import org.jetbrains.bazel.languages.projectview.sections.DeriveTargetsFromDirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.DirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.EnabledRulesSection
import org.jetbrains.bazel.languages.projectview.sections.GazelleTargetSection
import org.jetbrains.bazel.languages.projectview.sections.IdeJavaHomeOverrideSection
import org.jetbrains.bazel.languages.projectview.sections.ImportDepthSection
import org.jetbrains.bazel.languages.projectview.sections.ImportIjarsSection
import org.jetbrains.bazel.languages.projectview.sections.ImportRunConfigurationsSection
import org.jetbrains.bazel.languages.projectview.sections.IndexAllFilesInDirectoriesSection
import org.jetbrains.bazel.languages.projectview.sections.PreferClassJarsOverSourcelessJarsSection
import org.jetbrains.bazel.languages.projectview.sections.PythonDebugFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.RunConfigRunWithBazelSection
import org.jetbrains.bazel.languages.projectview.sections.ShardSyncSection
import org.jetbrains.bazel.languages.projectview.sections.ShardingApproachSection
import org.jetbrains.bazel.languages.projectview.sections.SyncFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.TargetShardSizeSection
import org.jetbrains.bazel.languages.projectview.sections.TargetsSection
import org.jetbrains.bazel.languages.projectview.sections.TestFlagsSection
import org.jetbrains.bazel.languages.projectview.sections.TestSourcesSection
import org.jetbrains.bazel.languages.projectview.sections.UseJetBrainsTestRunnerSection
import java.nio.file.Path

// Extension properties to provide convenient access to ProjectView sections
// These match the properties that were available in WorkspaceContext

val ProjectView.targets: List<ExcludableValue<Label>>
  @ApiStatus.Internal
  get() = getSection(TargetsSection.KEY) ?: emptyList()

val ProjectView.directories: List<ExcludableValue<Path>>
  @ApiStatus.Internal
  get() = getSection(DirectoriesSection.KEY) ?: emptyList()

val ProjectView.buildFlags: List<String>
  @ApiStatus.Internal
  get() = getSection(BuildFlagsSection.KEY) ?: emptyList()

val ProjectView.syncFlags: List<String>
  @ApiStatus.Internal
  get() = getSection(SyncFlagsSection.KEY) ?: emptyList()

val ProjectView.debugFlags: List<String>
  @ApiStatus.Internal
  get() = getSection(DebugFlagsSection.KEY) ?: emptyList()

val ProjectView.testFlags: List<String>
  @ApiStatus.Internal
  get() = getSection(TestFlagsSection.KEY) ?: emptyList()

val ProjectView.bazelBinary: Path?
  @ApiStatus.Internal
  get() = getSection(BazelBinarySection.KEY)

val ProjectView.allowManualTargetsSync: Boolean
  @ApiStatus.Internal
  get() = getSection(AllowManualTargetsSyncSection.KEY) ?: false

val ProjectView.deriveTargetsFromDirectories: Boolean
  @ApiStatus.Internal
  get() = getSection(DeriveTargetsFromDirectoriesSection.KEY) ?: false

val ProjectView.importDepth: Int
  @ApiStatus.Internal
  get() = getSection(ImportDepthSection.KEY) ?: -1

val ProjectView.enabledRules: List<String>
  @ApiStatus.Internal
  get() = getSection(EnabledRulesSection.KEY) ?: emptyList()

val ProjectView.ideJavaHomeOverride: Path?
  @ApiStatus.Internal
  get() = getSection(IdeJavaHomeOverrideSection.KEY)

val ProjectView.shardSync: Boolean
  @ApiStatus.Internal
  get() = getSection(ShardSyncSection.KEY) ?: false

val ProjectView.targetShardSize: Int
  @ApiStatus.Internal
  get() = getSection(TargetShardSizeSection.KEY) ?: 1000

val ProjectView.shardingApproach: String?
  @ApiStatus.Internal
  get() = getSection(ShardingApproachSection.KEY)?.name?.lowercase()

val ProjectView.importRunConfigurations: List<String>
  @ApiStatus.Internal
  get() = getSection(ImportRunConfigurationsSection.KEY)?.map { it.toString() } ?: emptyList()

val ProjectView.gazelleTarget: Label?
  @ApiStatus.Internal
  get() = getSection(GazelleTargetSection.KEY)

val ProjectView.indexAllFilesInDirectories: Boolean
  @ApiStatus.Internal
  get() = getSection(IndexAllFilesInDirectoriesSection.KEY) ?: false

val ProjectView.pythonDebugFlags: List<String>
  @ApiStatus.Internal
  get() = getSection(PythonDebugFlagsSection.KEY) ?: emptyList()

val ProjectView.importIjars: Boolean
  @ApiStatus.Internal
  get() = getSection(ImportIjarsSection.KEY) ?: false

val ProjectView.deriveInstrumentationFilterFromTargets: Boolean
  @ApiStatus.Internal
  get() = getSection(DeriveInstrumentationFilterFromTargetsSection.KEY) ?: false

val ProjectView.indexAdditionalFilesInDirectories: List<String>
  @ApiStatus.Internal
  get() = getSection(IndexAdditionalFilesInDirectoriesSection.KEY) ?: emptyList()

val ProjectView.useJetBrainsTestRunner: Boolean
  @ApiStatus.Internal
  get() = getSection(UseJetBrainsTestRunnerSection.KEY) ?: false

val ProjectView.preferClassJarsOverSourcelessJars: Boolean
  @ApiStatus.Internal
  get() = getSection(PreferClassJarsOverSourcelessJarsSection.KEY) ?: false

val ProjectView.runConfigRunWithBazel: Boolean
  @ApiStatus.Internal
  get() = getSection(RunConfigRunWithBazelSection.KEY) ?: false

val ProjectView.testSources: List<String>
  @ApiStatus.Internal
  get() = getSection(TestSourcesSection.KEY) ?: emptyList()
