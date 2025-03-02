package org.jetbrains.bazel.server.connection

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.server.client.BspClient
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.ui.console.BspConsoleService
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

/**
 * a temporary piece of data that signals built-in connection to reset to account for changes in configs
 * TODO: purge along with BSP complete removal
 */
private data class ConnectionResetConfig(val projectViewFile: Path?, val featureFlags: FeatureFlags)

private val log = logger<DefaultBazelServerConnection>()

class DotBazelBspCreator(projectPath: VirtualFile) : EnvironmentCreator(projectPath.toNioPath()) {
  override fun create() {
    createDotBazelBsp()
  }
}

class DefaultBazelServerConnection(private val project: Project) : BazelServerConnection {
  private val bspClient = createBspClient()
  private val workspaceRoot = project.rootDir.toNioPath()
  private val projectPath = VfsUtil.findFile(workspaceRoot, true) ?: error("Project doesn't exist")
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
    DotBazelBspCreator(projectPath).create()
  }

  private fun generateNewConnectionResetConfig(): ConnectionResetConfig =
    ConnectionResetConfig(
      projectViewFile = project.bazelProjectSettings.projectViewPath?.toAbsolutePath(),
      featureFlags = FeatureFlagsProvider.getFeatureFlags(),
    )

  private fun createBspClient(): BspClient {
    val bspConsoleService = BspConsoleService.getInstance(project)

    return BspClient(
      bspConsoleService.bspSyncConsole,
      bspConsoleService.bspBuildConsole,
      project,
    )
  }

  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T {
    log.debug("ensuring the connection is established")
    val newConnectionResetConfig = generateNewConnectionResetConfig()

    if (newConnectionResetConfig != connectionResetConfig) {
      connectionResetConfig = newConnectionResetConfig
      // TODO: change server's projectview path once the spaghetti is untangled
    }

    return task(server)
  }
}
