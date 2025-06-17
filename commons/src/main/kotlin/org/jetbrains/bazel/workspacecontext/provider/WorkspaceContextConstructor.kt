package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextConstructor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.slf4j.LoggerFactory
import java.nio.file.Path

class WorkspaceContextConstructor(
  workspaceRoot: Path,
  private val dotBazelBspDirPath: Path,
  projectViewPath: Path,
) : ExecutionContextConstructor<WorkspaceContext> {
  private val directoriesSpecExtractor = DirectoriesSpecExtractor(workspaceRoot, projectViewPath)

  private val log = LoggerFactory.getLogger(WorkspaceContextConstructor::class.java)

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
      experimentalTransitiveCompileTimeJarsTargetKinds =
        ExperimentalTransitiveCompileTimeJarsTargetKindsExtractor.fromProjectView(
          projectView,
        ),
      experimentalNoPruneTransitiveCompileTimeJarsPatterns =
        ExperimentalNoPruneTransitiveCompileTimeJarsPatternsExtractor.fromProjectView(
          projectView,
        ),
      experimentalPrioritizeLibrariesOverModulesTargetKinds =
        ExperimentalPrioritizeLibrariesOverModulesTargetKindsExtractor.fromProjectView(projectView),
      enableNativeAndroidRules = EnableNativeAndroidRulesExtractor.fromProjectView(projectView),
      androidMinSdkSpec = AndroidMinSdkSpecExtractor.fromProjectView(projectView),
      shardSync = ShardSyncSpecExtractor.fromProjectView(projectView),
      targetShardSize = TargetShardSizeSpecExtractor.fromProjectView(projectView),
      shardingApproachSpec = ShardingApproachSpecExtractor.fromProjectView(projectView),
      importRunConfigurations = ImportRunConfigurationsSpecExtractor.fromProjectView(projectView),
      gazelleTarget = GazelleTargetSpecExtractor.fromProjectView(projectView),
      indexAllFilesInDirectories = IndexAllFilesInDirectoriesSpecExtractor.fromProjectView(projectView),
    )
  }
}
