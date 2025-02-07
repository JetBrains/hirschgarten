package org.jetbrains.bsp.bazel.workspacecontext

import org.apache.logging.log4j.LogManager
import org.jetbrains.bazel.commons.label.assumeResolved
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContext
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextConstructor
import org.jetbrains.bsp.bazel.projectview.model.ProjectView
import java.nio.file.Path

/**
 * Representation of `ExecutionContext` used during server lifetime.
 *
 * @see org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContext
 */
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
  val enableNativeAndroidRules: EnableNativeAndroidRules,
  val androidMinSdkSpec: AndroidMinSdkSpec,
  val shardSync: ShardSyncSpec,
  val targetShardSize: TargetShardSizeSpec,
  val shardingApproachSpec: ShardingApproachSpec,
) : ExecutionContext()

/**
 * List of names of repositories that should be treated as internal because there are some targets that we want to be imported that
 * belong to them.
 */
val WorkspaceContext.externalRepositoriesTreatedAsInternal: List<String>
  get() =
    targets.values.mapNotNull {
      it
        .assumeResolved()
        .repo.repoName
        .takeIf { it.isNotEmpty() }
    }

class WorkspaceContextConstructor(workspaceRoot: Path, private val dotBazelBspDirPath: Path) :
  ExecutionContextConstructor<WorkspaceContext> {
  private val directoriesSpecExtractor = DirectoriesSpecExtractor(workspaceRoot)

  private val log = LogManager.getLogger(WorkspaceContextConstructor::class.java)

  override fun construct(projectView: ProjectView): WorkspaceContext {
    log.info("Constructing workspace context for: {}.", projectView)

    return WorkspaceContext(
      targets = TargetsSpecExtractor.fromProjectView(projectView),
      directories = directoriesSpecExtractor.fromProjectView(projectView),
      buildFlags = BuildFlagsSpecExtractor.fromProjectView(projectView),
      syncFlags = SyncFlagsSpecExtractor.fromProjectView(projectView),
      bazelBinary = BazelBinarySpecExtractor.fromProjectView(projectView),
      allowManualTargetsSync = AllowManualTargetsSyncSpecExtractor.fromProjectView(projectView),
      dotBazelBspDirPath = DotBazelBspDirPathSpec(dotBazelBspDirPath),
      importDepth = ImportDepthSpecExtractor.fromProjectView(projectView),
      enabledRules = EnabledRulesSpecExtractor.fromProjectView(projectView),
      ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpecExtractor.fromProjectView(projectView),
      experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJarsExtractor.fromProjectView(projectView),
      enableNativeAndroidRules = EnableNativeAndroidRulesExtractor.fromProjectView(projectView),
      androidMinSdkSpec = AndroidMinSdkSpecExtractor.fromProjectView(projectView),
      shardSync = ShardSyncSpecExtractor.fromProjectView(projectView),
      targetShardSize = TargetShardSizeSpecExtractor.fromProjectView(projectView),
      shardingApproachSpec = ShardingApproachSpecExtractor.fromProjectView(projectView),
    )
  }
}

val WorkspaceContext.extraFlags: List<String>
  get() =
    if (enableNativeAndroidRules.value) {
      listOf(
        BazelFlag.experimentalGoogleLegacyApi(),
        BazelFlag.experimentalEnableAndroidMigrationApis(),
      )
    } else {
      emptyList()
    }
