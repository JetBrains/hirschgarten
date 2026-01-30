package org.jetbrains.bazel.server.connection

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.BazelBspServer
import org.jetbrains.bazel.server.bsp.BspServerApi
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

suspend fun startServer(
  project: Project,
  client: JoinedBuildClient,
  workspaceRoot: Path,
  workspaceContext: WorkspaceContext,
  featureFlags: FeatureFlags,
): BspServerApi {
  val bspInfo = BspInfo(workspaceRoot)
  val bspServer = BazelBspServer(project, bspInfo, workspaceContext, workspaceRoot)
  val bspClientLogger = BspClientLogger(client)
  val bazelRunner = BazelRunner(bspClientLogger, bspServer.workspaceRoot)
  val bazelInfo = bspServer.createBazelInfo(bazelRunner, workspaceContext)
  bazelInfo.release.deprecated()?.let { bspClientLogger.warn(it + " Sync might give incomplete results.") }
  val bazelPathsResolver = BazelPathsResolver(bazelInfo)
  val services =
    bspServer.bspServerData(
      bspClientLogger,
      bazelRunner,
      bspServer.workspaceRoot,
      client,
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
