package org.jetbrains.bazel.server.connection

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.BazelBspServer
import org.jetbrains.bazel.server.bsp.BspServerApi
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

suspend fun startServer(
  client: JoinedBuildClient,
  workspaceRoot: Path,
  workspaceContext: WorkspaceContext,
  featureFlags: FeatureFlags,
): BspServerApi {
  val bspInfo = BspInfo(workspaceRoot)
  val bspServer = BazelBspServer(bspInfo, workspaceContext, workspaceRoot)
  val bspClientLogger = BspClientLogger(client)
  val bazelRunner = BazelRunner(bspClientLogger, bspServer.workspaceRoot)
  bspServer.verifyBazelVersion(bazelRunner, workspaceContext)
  val bazelInfo = bspServer.createBazelInfo(bazelRunner, workspaceContext)
  bazelRunner.bazelInfo = bazelInfo
  val bazelPathsResolver = BazelPathsResolver(bazelInfo)
  val compilationManager =
    BazelBspCompilationManager(bazelRunner, bazelPathsResolver, client, bspServer.workspaceRoot)
  val services =
    bspServer.bspServerData(
      bspClientLogger,
      bazelRunner,
      compilationManager,
      bazelInfo,
      workspaceContext,
      featureFlags,
      bazelPathsResolver,
    )
  val bspServerApi =
    BspServerApi(
      services.projectSyncService,
      services.executeService,
      workspaceContext,
      bazelPathsResolver,
    )
  return bspServerApi
}
