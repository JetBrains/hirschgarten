package org.jetbrains.bazel.server.connection

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import org.jetbrains.bazel.server.client.BazelClient
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

private data class ConnectionResetConfig(val featureFlags: FeatureFlags)

private val log = logger<DefaultBazelServerConnection>()

class DefaultBazelServerConnection(private val project: Project) : BazelServerConnection {
  private val bspClient = createBspClient()
  private val workspaceRoot = project.rootDir.toNioPath()
  private var connectionResetConfig = generateNewConnectionResetConfig()
  private val environmentCreator = EnvironmentCreator(workspaceRoot)
  private var server =
    runBlocking {
      val workspaceContextProvider = project.service<WorkspaceContextProvider>()
      val workspaceContext = workspaceContextProvider.readWorkspaceContext()
      startServer(
        bspClient,
        workspaceRoot = workspaceRoot,
        workspaceContext = workspaceContext,
        featureFlags = FeatureFlagsProvider.getFeatureFlags(project),
      )
    }

  init {
    environmentCreator.create()
  }

  private fun generateNewConnectionResetConfig(): ConnectionResetConfig =
    ConnectionResetConfig(
      featureFlags = FeatureFlagsProvider.getFeatureFlags(project),
    )

  private fun createBspClient(): BazelClient {
    val consoleService = ConsoleService.getInstance(project)

    return BazelClient(
      consoleService.syncConsole,
      consoleService.buildConsole,
      project,
    )
  }

  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T {
    // ensure `.bazelbsp` directory exists and functions
    environmentCreator.create()

    // reset server if needed
    resetServerIfNeeded()

    // update connection reset config if needed
    val newConnectionResetConfig = generateNewConnectionResetConfig()

    if (newConnectionResetConfig != connectionResetConfig) {
      connectionResetConfig = newConnectionResetConfig
      // Server now receives pre-parsed WorkspaceContext, so we need to restart the server
      // to pick up any project view changes
      val workspaceContextProvider = project.service<WorkspaceContextProvider>()
      val workspaceContext = workspaceContextProvider.readWorkspaceContext()
      server = startServer(bspClient, workspaceRoot, workspaceContext, connectionResetConfig.featureFlags)
    }

    return task(server)
  }

  private suspend fun resetServerIfNeeded() {
    if (project.service<BazelVersionCheckerService>().updateCurrentVersion()) {
      log.info("Resetting Bazel server")
      val workspaceContextProvider = project.service<WorkspaceContextProvider>()
      val workspaceContext = workspaceContextProvider.readWorkspaceContext()
      server = startServer(bspClient, workspaceRoot, workspaceContext, connectionResetConfig.featureFlags)
    }
  }
}
