package org.jetbrains.bazel.server.connection

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.server.client.BazelClient
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

private data class ConnectionResetConfig(val projectViewFile: Path?, val featureFlags: FeatureFlags)

private val log = logger<DefaultBazelServerConnection>()

class DefaultBazelServerConnection(private val project: Project) : BazelServerConnection {
  private val bspClient = createBspClient()
  private val workspaceRoot = project.rootDir.toNioPath()
  private var connectionResetConfig = generateNewConnectionResetConfig()
  private val server =
    runBlocking {
      startServer(
        bspClient,
        workspaceRoot = workspaceRoot,
        projectViewFile = connectionResetConfig.projectViewFile,
        featureFlags = FeatureFlagsProvider.getFeatureFlags(),
      )
    }

  init {
    EnvironmentCreator(workspaceRoot).create()
  }

  private fun generateNewConnectionResetConfig(): ConnectionResetConfig =
    ConnectionResetConfig(
      projectViewFile = project.bazelProjectSettings.projectViewPath?.toAbsolutePath(),
      featureFlags = FeatureFlagsProvider.getFeatureFlags(),
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
    log.debug("ensuring the connection is established")
    val newConnectionResetConfig = generateNewConnectionResetConfig()

    if (newConnectionResetConfig != connectionResetConfig) {
      connectionResetConfig = newConnectionResetConfig
      server.workspaceContextProvider.featureFlags = newConnectionResetConfig.featureFlags
      newConnectionResetConfig.projectViewFile?.let { server.workspaceContextProvider.projectViewPath = it }
    }

    return task(server)
  }
}
