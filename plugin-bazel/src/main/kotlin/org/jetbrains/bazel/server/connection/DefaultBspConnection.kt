package org.jetbrains.bazel.server.connection

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.server.client.BspClient
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.ui.console.BspConsoleService
import org.jetbrains.bazel.ui.console.ids.CONNECT_TASK_ID
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

/**
 * a temporary piece of data that signals built-in connection to reset to account for changes in configs
 * TODO: purge along with BSP complete removal
 */
private data class ConnectionResetConfig(val projectViewFile: Path?, val featureFlags: FeatureFlags)

private val log = logger<DefaultBspConnection>()

class DotBazelBspCreator(projectPath: VirtualFile) : EnvironmentCreator(projectPath.toNioPath()) {
  override fun create() {
    createDotBazelBsp()
  }
}

class DefaultBspConnection(private val project: Project) : BspConnection {
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

  private suspend fun connectBuiltIn(server: JoinedBuildServer, featureFlags: FeatureFlags) {
    coroutineScope {
      val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
      bspSyncConsole.startTask(
        taskId = CONNECT_TASK_ID,
        title = BspPluginBundle.message("console.task.auto.connect.title"),
        message = BspPluginBundle.message("console.task.auto.connect.in.progress"),
        cancelAction = { coroutineContext.cancel() },
      )
      bspSyncConsole.addMessage(
        CONNECT_TASK_ID,
        BspPluginBundle.message("console.message.initialize.server.in.progress"),
      )
      bspSyncConsole.addMessage(
        CONNECT_TASK_ID,
        BspPluginBundle.message("console.message.initialize.server.success"),
      )
      bspSyncConsole.finishTask(CONNECT_TASK_ID, BspPluginBundle.message("console.task.auto.connect.success"))
    }
  }

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
      connectBuiltIn(server, connectionResetConfig.featureFlags)
      // TODO: change server's projectview path once the spaghetti is untangled
    }

    return task(server)
  }
}
