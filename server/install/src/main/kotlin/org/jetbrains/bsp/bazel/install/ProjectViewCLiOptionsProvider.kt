package org.jetbrains.bsp.bazel.install

import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.install.cli.CliOptions
import org.jetbrains.bsp.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bsp.bazel.projectview.generator.DefaultProjectViewGenerator
import org.jetbrains.bsp.bazel.projectview.model.ProjectView
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewBazelBinarySection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewBuildFlagsSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewDeriveTargetsFromDirectoriesSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewEnabledRulesSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewIdeJavaHomeOverrideSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewImportDepthSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewSyncFlagsSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ShardSyncSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ShardingApproachSection
import org.jetbrains.bsp.bazel.projectview.model.sections.TargetShardSizeSection
import java.nio.file.Path
import kotlin.io.path.Path

object ProjectViewCLiOptionsProvider {
  fun generateProjectViewAndSave(cliOptions: CliOptions, generatedProjectViewFilePath: Path): ProjectView {
    val projectView = toProjectView(cliOptions.projectViewCliOptions)
    DefaultProjectViewGenerator.generatePrettyStringAndSaveInFile(projectView, generatedProjectViewFilePath)
    return projectView
  }

  private fun toProjectView(projectViewCliOptions: ProjectViewCliOptions?): ProjectView =
    ProjectView(
      bazelBinary = toBazelBinarySection(projectViewCliOptions),
      targets = toTargetsSection(projectViewCliOptions),
      buildFlags = toBuildFlagsSection(projectViewCliOptions),
      syncFlags = toSyncFlagsSection(projectViewCliOptions),
      directories = toDirectoriesSection(projectViewCliOptions),
      deriveTargetsFromDirectories = toDeriveTargetFlagSection(projectViewCliOptions),
      importDepth = toImportDepthSection(projectViewCliOptions),
      allowManualTargetsSync = toAllowManualTargetsSyncSection(projectViewCliOptions),
      enabledRules = toEnabledRulesSection(projectViewCliOptions),
      ideJavaHomeOverride = toIdeJavaHomeOverrideSection(projectViewCliOptions),
      shardSync = toShardSyncSection(projectViewCliOptions),
      targetShardSize = toTargetShardSizeSection(projectViewCliOptions),
      shardingApproach = toShardingStrategy(projectViewCliOptions),
    )

  private fun toBazelBinarySection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewBazelBinarySection? =
    projectViewCliOptions?.bazelBinary?.let(::ProjectViewBazelBinarySection)

  private fun toTargetsSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewTargetsSection? =
    when {
      projectViewCliOptions == null -> null
      projectViewCliOptions.targets != null || projectViewCliOptions.excludedTargets != null ->
        toTargetsSectionNotNull(projectViewCliOptions)

      else -> null
    }

  private fun toTargetsSectionNotNull(projectViewCliOptions: ProjectViewCliOptions): ProjectViewTargetsSection {
    val includedTargets = projectViewCliOptions.targets.orEmpty().map { Label.parse(it) }
    val excludedTargets = projectViewCliOptions.excludedTargets.orEmpty().map { Label.parse(it) }

    return ProjectViewTargetsSection(includedTargets, excludedTargets)
  }

  private fun toAllowManualTargetsSyncSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewAllowManualTargetsSyncSection? =
    projectViewCliOptions?.allowManualTargetsSync?.let(::ProjectViewAllowManualTargetsSyncSection)

  private fun toDirectoriesSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewDirectoriesSection? =
    when {
      projectViewCliOptions == null -> null
      projectViewCliOptions.directories != null || projectViewCliOptions.excludedDirectories != null ->
        toDirectoriesSectionNotNull(projectViewCliOptions)

      else -> null
    }

  private fun toDirectoriesSectionNotNull(projectViewCliOptions: ProjectViewCliOptions): ProjectViewDirectoriesSection {
    val includedDirectories = projectViewCliOptions.directories.orEmpty().map { Path(it) }
    val excludedDirectories = projectViewCliOptions.excludedDirectories.orEmpty().map { Path(it) }

    return ProjectViewDirectoriesSection(includedDirectories, excludedDirectories)
  }

  private fun toImportDepthSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewImportDepthSection? =
    projectViewCliOptions?.importDepth?.let(::ProjectViewImportDepthSection)

  private fun toBuildFlagsSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewBuildFlagsSection? =
    projectViewCliOptions?.buildFlags?.let { ProjectViewBuildFlagsSection(it) }

  private fun toSyncFlagsSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewSyncFlagsSection? =
    projectViewCliOptions?.syncFlags?.let { ProjectViewSyncFlagsSection(it) }

  private fun toEnabledRulesSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewEnabledRulesSection? =
    projectViewCliOptions?.enabledRules?.let { ProjectViewEnabledRulesSection(it) }

  private fun toIdeJavaHomeOverrideSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewIdeJavaHomeOverrideSection? =
    projectViewCliOptions?.ideJavaHomeOverride?.let { ProjectViewIdeJavaHomeOverrideSection(it) }

  private fun toDeriveTargetFlagSection(projectViewCliOptions: ProjectViewCliOptions?): ProjectViewDeriveTargetsFromDirectoriesSection? =
    projectViewCliOptions?.deriveTargetsFromDirectories?.let(::ProjectViewDeriveTargetsFromDirectoriesSection)

  private fun toShardSyncSection(projectViewCliOptions: ProjectViewCliOptions?): ShardSyncSection? =
    projectViewCliOptions?.shardSync?.let(::ShardSyncSection)

  private fun toTargetShardSizeSection(projectViewCliOptions: ProjectViewCliOptions?): TargetShardSizeSection? =
    projectViewCliOptions?.targetShardSize?.let(::TargetShardSizeSection)

  private fun toShardingStrategy(projectViewCliOptions: ProjectViewCliOptions?): ShardingApproachSection? =
    projectViewCliOptions?.shardApproach?.let(::ShardingApproachSection)
}
