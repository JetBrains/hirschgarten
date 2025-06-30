package org.jetbrains.bazel.server.connection

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.server.BazelBspServer
import org.jetbrains.bazel.server.bsp.BspServerApi
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.process.ServerProcessSpawner
import org.jetbrains.bazel.server.telemetry.ServerTelemetryManager
import org.jetbrains.bazel.startup.IntellijBidirectionalMap
import org.jetbrains.bazel.workspacecontext.provider.DefaultWorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

suspend fun startServer(
  client: JoinedBuildClient,
  workspaceRoot: Path,
  projectViewFile: Path?,
  featureFlags: FeatureFlags,
): BspServerApi {
  val bspInfo = BspInfo(workspaceRoot)
  val workspaceContextProvider =
    DefaultWorkspaceContextProvider(
      workspaceRoot = workspaceRoot,
      projectViewPath = projectViewFile ?: workspaceRoot.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME),
      dotBazelBspDirPath = bspInfo.bazelBspDir(),
      featureFlags = featureFlags,
    )
  // Run it here to force the workspace context to be initialized
  // It should download bazelisk if bazel is missing
  val workspaceContext = workspaceContextProvider.readWorkspaceContext()

  // Initialize ProcessSpawner, TelemetryManager, EnvironmentProvider, and BidirectionalMap for server environment
  ProcessSpawner.provideProcessSpawner(ServerProcessSpawner)
  TelemetryManager.provideTelemetryManager(ServerTelemetryManager)
  EnvironmentProvider.provideEnvironmentProvider(object : EnvironmentProvider {
    override fun getValue(name: String): String? = System.getenv(name)
  })
  BidirectionalMap.provideBidirectionalMapFactory {
    IntellijBidirectionalMap<Any?, Any?>()
  }

  val bspServer = BazelBspServer(bspInfo, workspaceContextProvider, workspaceRoot)
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
      bspServer.workspaceContextProvider,
      bazelPathsResolver,
      featureFlags,
    )
  val bspServerApi =
    BspServerApi(
      services.projectSyncService,
      services.executeService,
      workspaceContextProvider,
    )
  return bspServerApi
}
