package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bazel.workspacecontext.AndroidMinSdkSpec
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.EnableNativeAndroidRules
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bazel.workspacecontext.GazelleTargetSpec
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bazel.workspacecontext.ImportIjarsSpec
import org.jetbrains.bazel.workspacecontext.ImportRunConfigurationsSpec
import org.jetbrains.bazel.workspacecontext.IndexAllFilesInDirectoriesSpec
import org.jetbrains.bazel.workspacecontext.NoPruneTransitiveCompileTimeJarsPatternsSpec
import org.jetbrains.bazel.workspacecontext.PrioritizeLibrariesOverModulesTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.PythonCodeGeneratorRuleNamesSpec
import org.jetbrains.bazel.workspacecontext.ShardSyncSpec
import org.jetbrains.bazel.workspacecontext.ShardingApproachSpec
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.jetbrains.bazel.workspacecontext.TargetShardSizeSpec
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.TransitiveCompileTimeJarsTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BazelProject
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import kotlin.io.path.Path

private val mockWorkspaceContext =
  WorkspaceContext(
    targets = TargetsSpec(listOf(Label.parse("//...")), emptyList()),
    directories = DirectoriesSpec(listOf(Path(".")), emptyList()),
    buildFlags = BuildFlagsSpec(emptyList()),
    syncFlags = SyncFlagsSpec(emptyList()),
    bazelBinary = BazelBinarySpec(Path("bazel")),
    allowManualTargetsSync = AllowManualTargetsSyncSpec(true),
    dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
    importDepth = ImportDepthSpec(-1),
    enabledRules = EnabledRulesSpec(emptyList()),
    ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
    experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(false),
    experimentalTransitiveCompileTimeJarsTargetKinds = TransitiveCompileTimeJarsTargetKindsSpec(emptyList()),
    experimentalNoPruneTransitiveCompileTimeJarsPatterns = NoPruneTransitiveCompileTimeJarsPatternsSpec(emptyList()),
    experimentalPrioritizeLibrariesOverModulesTargetKinds = PrioritizeLibrariesOverModulesTargetKindsSpec(emptyList()),
    enableNativeAndroidRules = EnableNativeAndroidRules(false),
    androidMinSdkSpec = AndroidMinSdkSpec(null),
    shardSync = ShardSyncSpec(false),
    targetShardSize = TargetShardSizeSpec(1000),
    shardingApproachSpec = ShardingApproachSpec(null),
    importRunConfigurations = ImportRunConfigurationsSpec(emptyList()),
    gazelleTarget = GazelleTargetSpec(null),
    indexAllFilesInDirectories = IndexAllFilesInDirectoriesSpec(false),
    pythonCodeGeneratorRuleNames = PythonCodeGeneratorRuleNamesSpec(emptyList()),
    importIjarsSpec = ImportIjarsSpec(false),
  )

private val mockBuildServer =
  BuildServerMock(
    bazelProject = BazelProject(emptyMap(), false),
    workspaceBuildTargetsResult = WorkspaceBuildTargetsResult(emptyList()),
    workspaceDirectoriesResult = WorkspaceDirectoriesResult(emptyList(), emptyList()),
    workspaceLibrariesResult = WorkspaceLibrariesResult(emptyList()),
    jvmBinaryJarsResult = JvmBinaryJarsResult(emptyList()),
    workspaceBazelRepoMappingResult = WorkspaceBazelRepoMappingResult(emptyMap(), emptyMap()),
    dependencySourcesResult = DependencySourcesResult(emptyList()),
    workspaceContextResult = mockWorkspaceContext,
  )

private class BazelServerConnectionMock : BazelServerConnection {
  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T = task(mockBuildServer)
}

class BazelServerServiceMock : BazelServerService {
  override val connection: BazelServerConnection = BazelServerConnectionMock()
}
