package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import java.nio.file.Path

/**
 * Simplified WorkspaceContext using plain typed values that match the new ProjectView section types.
 * This is now a compatibility layer that can be constructed from the new ProjectView implementation.
 */
data class WorkspaceContext(
  /**
   * Targets (included and excluded) on which the user wants to work.
   */
  val targets: List<ExcludableValue<Label>>,
  /**
   * Directories (included and excluded) in the project.
   */
  val directories: List<ExcludableValue<Path>>,
  /**
   * Build flags which should be added to each bazel call.
   */
  val buildFlags: List<String>,
  /**
   * Sync flags which should be added to sync call.
   */
  val syncFlags: List<String>,
  /**
   * Debug flags which should be added to bazel build/run call for debugging.
   */
  val debugFlags: List<String>,
  /**
   * Path to bazel which should be used in the bazel runner.
   */
  val bazelBinary: Path?,
  /**
   * If true targets with `manual` tag will be built
   */
  val allowManualTargetsSync: Boolean,
  /**
   * Path to the `.bazelbsp` dir in the project root
   */
  val dotBazelBspDirPath: Path,
  /**
   * Parameter determining targets importing depth
   */
  val importDepth: Int,
  /**
   * Parameter determining which rules should be used by Bazel BSP, if empty Bazel is queried.
   */
  val enabledRules: List<String>,
  /**
   * Parameter determining the java home path that should be used with the local IDE
   */
  val ideJavaHomeOverride: Path?,
  val shardSync: Boolean,
  val targetShardSize: Int,
  val shardingApproach: String?,
  val importRunConfigurations: List<String>,
  val gazelleTarget: Label?,
  val indexAllFilesInDirectories: Boolean,
  val pythonCodeGeneratorRuleNames: List<String>,
  val importIjars: Boolean,
  val deriveInstrumentationFilterFromTargets: Boolean,
  val indexAdditionalFilesInDirectories: List<String>,
)

/**
 * List of names of repositories that should be treated as internal because there are some targets that we want to be imported that
 * belong to them.
 */
val WorkspaceContext.externalRepositoriesTreatedAsInternal: List<String>
  get() =
    targets
      .filter { it.isIncluded() }
      .mapNotNull { excludableValue ->
        excludableValue.value
          .assumeResolved()
          .repo.repoName
          .takeIf { repoName -> repoName.isNotEmpty() }
      }.distinct()
