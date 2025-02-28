package org.jetbrains.bazel.server.connection

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.BazelBspServer
import org.jetbrains.bazel.server.bsp.BazelBspServerLifetime
import org.jetbrains.bazel.server.bsp.BspServerApi
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.workspacecontext.DefaultWorkspaceContextProvider
import org.jetbrains.bsp.protocol.InitializeBuildParams
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

class Connection(
  installationDirectory: Path,
  projectViewFile: Path?,
  workspace: Path,
  client: JoinedBuildClient,
) {
  val server =
    startServer(
      client,
      workspace,
      installationDirectory,
      projectViewFile,
    )
}

private fun startServer(
  client: JoinedBuildClient,
  workspace: Path,
  directory: Path,
  projectViewFile: Path?,
): JoinedBuildServer {
  val bspInfo = BspInfo(directory)
  val workspaceContextProvider =
    DefaultWorkspaceContextProvider(
      workspaceRoot = workspace,
      projectViewPath = projectViewFile ?: directory.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME),
      dotBazelBspDirPath = bspInfo.bazelBspDir(),
    )
  val bspServer = BazelBspServer(bspInfo, workspaceContextProvider, workspace)
  val bspServerApi =
    BspServerApi { client: JoinedBuildClient, initializeBuildParams: InitializeBuildParams ->
      val bspClientLogger = BspClientLogger(client)
      val bazelRunner = BazelRunner(bspServer.workspaceContextProvider, bspClientLogger, bspServer.workspaceRoot)
      bspServer.verifyBazelVersion(bazelRunner)
      val bazelInfo = bspServer.createBazelInfo(bazelRunner)
      bazelRunner.bazelInfo = bazelInfo
      val bazelPathsResolver = BazelPathsResolver(bazelInfo)
      val compilationManager =
        BazelBspCompilationManager(bazelRunner, bazelPathsResolver, client, bspServer.workspaceRoot)
      bspServer.bspServerData(
        initializeBuildParams,
        bspClientLogger,
        bazelRunner,
        compilationManager,
        bazelInfo,
        bspServer.workspaceContextProvider,
        bazelPathsResolver,
      )
    }
  val serverLifetime = BazelBspServerLifetime(bspServer.workspaceContextProvider)
  bspServerApi.initialize(client, serverLifetime)
  return bspServerApi
}
