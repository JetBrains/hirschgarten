package org.jetbrains.bazel.server

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.bsp.BazelServices
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectProvider
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.jetbrains.bazel.server.sync.ProjectSyncService
import org.jetbrains.bazel.server.sync.TargetInfoReader
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

class BazelBspServer(
  private val project: Project,
  private val bspInfo: BspInfo,
  val workspaceContext: WorkspaceContext,
  val workspaceRoot: Path,
) {
  fun bspServerData(
    bspClientLogger: BspClientLogger,
    bazelRunner: BazelRunner,
    workspaceRoot: Path,
    client: JoinedBuildClient,
    bazelInfo: BazelInfo,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
    bazelPathsResolver: BazelPathsResolver,
  ): BazelServices {
    val executeService =
      ExecuteService(
        project = project,
        workspaceRoot = workspaceRoot,
        client = client,
        bazelRunner = bazelRunner,
        workspaceContext = workspaceContext,
        bazelPathsResolver = bazelPathsResolver,
      )
    val projectProvider =
      createProjectProvider(
        bspInfo = bspInfo,
        bazelInfo = bazelInfo,
        workspaceContext = workspaceContext,
        featureFlags = featureFlags,
        bazelRunner = bazelRunner,
        bazelPathsResolver = bazelPathsResolver,
        executeService = executeService,
        bspClientLogger = bspClientLogger,
      )
    val bspProjectMapper =
      BspProjectMapper(
        bazelRunner = bazelRunner,
        bspInfo = bspInfo,
      )
    val firstPhaseTargetToBspMapper = FirstPhaseTargetToBspMapper()
    val projectSyncService =
      ProjectSyncService(bspProjectMapper, firstPhaseTargetToBspMapper, projectProvider, bazelInfo, workspaceContext)

    return BazelServices(
      projectSyncService,
      executeService,
    )
  }

  suspend fun createBazelInfo(bazelRunner: BazelRunner, workspaceContext: WorkspaceContext): BazelInfo {
    val bazelDataResolver = BazelInfoResolver(bazelRunner)
    return bazelDataResolver.resolveBazelInfo(workspaceContext)
  }

  fun createProjectProvider(
    bspInfo: BspInfo,
    bazelInfo: BazelInfo,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
    bazelRunner: BazelRunner,
    bazelPathsResolver: BazelPathsResolver,
    executeService: ExecuteService,
    bspClientLogger: BspClientLogger,
  ): ProjectProvider {
    val aspectsResolver =
      InternalAspectsResolver(
        bspInfo = bspInfo,
      )

    val bazelBspAspectsManager =
      BazelBspAspectsManager(
        executeService = executeService,
        aspectsResolver = aspectsResolver,
        bazelRelease = bazelInfo.release,
      )
    val bazelToolchainManager = BazelToolchainManager()
    val bazelBspLanguageExtensionsGenerator = BazelBspLanguageExtensionsGenerator(aspectsResolver)
    val targetInfoReader = TargetInfoReader(bspClientLogger)

    val projectResolver =
      ProjectResolver(
        bazelBspAspectsManager = bazelBspAspectsManager,
        bazelToolchainManager = bazelToolchainManager,
        bazelBspLanguageExtensionsGenerator = bazelBspLanguageExtensionsGenerator,
        workspaceContext = workspaceContext,
        featureFlags = featureFlags,
        targetInfoReader = targetInfoReader,
        bazelInfo = bazelInfo,
        bazelRunner = bazelRunner,
        bazelPathsResolver = bazelPathsResolver,
        bspClientLogger = bspClientLogger,
      )
    val firstPhaseProjectResolver =
      FirstPhaseProjectResolver(
        workspaceRoot = workspaceRoot,
        bazelRunner = bazelRunner,
        workspaceContext = workspaceContext,
        bazelInfo = bazelInfo,
        bspClientLogger = bspClientLogger,
      )
    return ProjectProvider(projectResolver, firstPhaseProjectResolver)
  }
}
