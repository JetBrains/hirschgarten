package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.label.assumeResolved

/**
 * Base `WorkspaceContext` entity class - you need to extend it or
 * `WorkspaceContextListEntity` or `WorkspaceContextSingletonEntity` if you want to create your entity.
 *
 * @see WorkspaceContextExcludableListEntity
 * @see WorkspaceContextSingletonEntity
 */
abstract class WorkspaceContextEntity

/** `ProjectViewToWorkspaceContextEntityMapper` mapping failed? Return ('throw') it. */
class WorkspaceContextEntityExtractorException(entityName: String, message: String) :
  Exception("Mapping project view into '$entityName' failed! $message")

data class WorkspaceContext(
  /**
   * Targets (included and excluded) on which the user wants to work.
   *
   *
   * Obtained from `ProjectView` simply by mapping 'targets' section
   * or derived from 'directories' if 'derive_targets_from_directories' is true.
   */
  val targets: TargetsSpec,
  /**
   * Directories (included and excluded) in the project.
   *
   * Obtained from 'ProjectView' simply by mapping 'directories' section if not null,
   * otherwise the whole project is included (project root is included).
   */
  val directories: DirectoriesSpec,
  /**
   * Build flags which should be added to each bazel call.
   *
   * Obtained from `ProjectView` simply by mapping `build_flags` section.
   */
  val buildFlags: BuildFlagsSpec,
  /**
   * Sync flags which should be added to sync call.
   *
   * Obtained from `ProjectView` simply by mapping `sync_flags` section.
   */
  val syncFlags: SyncFlagsSpec,
  /**
   * Debug flags which should be added to bazel build/run call for debugging.
   *
   * Obtained from `ProjectView` simply by mapping `debug_flags` section.
   */
  val debugFlags: DebugFlagsSpec,
  /**
   * Path to bazel which should be used in the bazel runner.
   *
   * Obtained from `ProjectView` if not null, otherwise deducted from `PATH`.
   */
  val bazelBinary: BazelBinarySpec,
  /**
   * If true targets with `manual` tag will be built
   *
   * Obtained from `ProjectView` simply by mapping 'allow_manual_targets_sync' section.
   */
  val allowManualTargetsSync: AllowManualTargetsSyncSpec,
  /**
   * Path to the `.bazelbsp` dir in the project root
   *
   * Deducted from working directory.
   */
  val dotBazelBspDirPath: DotBazelBspDirPathSpec,
  /**
   * Parameter determining targets importing depth
   *
   * Obtained from `ProjectView` simply by mapping `import_depth` section.
   */
  val importDepth: ImportDepthSpec,
  /**
   * Parameter determining which rules should be used by Bazel BSP, if empty Bazel is queried.
   *
   * Obtained from `ProjectView` simply by mapping `enabled_rules` section.
   */
  val enabledRules: EnabledRulesSpec,
  /**
   * Parameter determining the java home path that should be used with the local IDE
   *
   * Obtained from `ProjectView` simply by mapping `ide_java_home_override` section.
   */
  val ideJavaHomeOverrideSpec: IdeJavaHomeOverrideSpec,
  val experimentalAddTransitiveCompileTimeJars: ExperimentalAddTransitiveCompileTimeJars,
  val experimentalTransitiveCompileTimeJarsTargetKinds: TransitiveCompileTimeJarsTargetKindsSpec,
  val experimentalNoPruneTransitiveCompileTimeJarsPatterns: NoPruneTransitiveCompileTimeJarsPatternsSpec,
  val experimentalPrioritizeLibrariesOverModulesTargetKinds: PrioritizeLibrariesOverModulesTargetKindsSpec,
  val enableNativeAndroidRules: EnableNativeAndroidRules,
  val androidMinSdkSpec: AndroidMinSdkSpec,
  val shardSync: ShardSyncSpec,
  val targetShardSize: TargetShardSizeSpec,
  val shardingApproachSpec: ShardingApproachSpec,
  val importRunConfigurations: ImportRunConfigurationsSpec,
  val gazelleTarget: GazelleTargetSpec,
  val indexAllFilesInDirectories: IndexAllFilesInDirectoriesSpec,
  val pythonCodeGeneratorRuleNames: PythonCodeGeneratorRuleNamesSpec,
  val importIjarsSpec: ImportIjarsSpec,
)

/**
 * List of names of repositories that should be treated as internal because there are some targets that we want to be imported that
 * belong to them.
 */
val WorkspaceContext.externalRepositoriesTreatedAsInternal: List<String>
  get() =
    targets.values
      .mapNotNull {
        it
          .assumeResolved()
          .repo.repoName
          .takeIf { repoName -> repoName.isNotEmpty() }
      }.distinct()
