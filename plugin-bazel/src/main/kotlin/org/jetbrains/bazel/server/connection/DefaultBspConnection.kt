package org.jetbrains.bazel.server.connection

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.server.client.BspClient
import org.jetbrains.bazel.server.client.GenericConnection
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.ui.console.BspConsoleService
import org.jetbrains.bazel.ui.console.ids.CONNECT_TASK_ID
import org.jetbrains.bsp.protocol.InitializeBuildParams
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

/**
 * a temporary piece of data that signals built-in connection to reset to account for changes in configs
 * TODO: purge along with BSP complete removal
 */
private data class ConnectionResetConfig(val projectViewFile: Path?, val initializeBuildData: InitializeBuildParams)

private val log = logger<DefaultBspConnection>()

class DotBazelBspCreator(projectPath: VirtualFile) : EnvironmentCreator(projectPath.toNioPath()) {
  override fun create() {
    createDotBazelBsp()
  }
}

class DefaultBspConnection(private val project: Project) : BspConnection {
  @Volatile
  private var server: JoinedBuildServer? = null

  private var connectionResetConfig: ConnectionResetConfig? = null

  private val mutex = Mutex()

  override suspend fun connect() {
    mutex.withLock {
      ensureConnected()
    }
  }

  private suspend fun ensureConnected() {
    log.debug("ensuring the connection is established")
    val bspClient = createBspClient()
    val newConnectionResetConfig = generateNewConnectionResetConfig()
    if (!isConnected() || newConnectionResetConfig != connectionResetConfig) {
      connectionResetConfig = newConnectionResetConfig
      val inMemoryConnection =
        object : GenericConnection {
          val installationDirectory = project.rootDir.toNioPath()
          val conn =
            Connection(
              installationDirectory,
              newConnectionResetConfig.projectViewFile,
              installationDirectory,
              bspClient,
            )
          val projectPath = VfsUtil.findFile(installationDirectory, true) ?: error("Project doesn't exist")

          init {
            DotBazelBspCreator(projectPath).create()
          }

          override val server: JoinedBuildServer
            get() = conn.server
        }
      connectBuiltIn(inMemoryConnection)
    }
  }

  private fun generateNewConnectionResetConfig(): ConnectionResetConfig =
    ConnectionResetConfig(
      projectViewFile = project.bazelProjectSettings.projectViewPath?.toAbsolutePath(),
      initializeBuildData =
        InitializeBuildParams(
          clientClassesRootDir = "${project.rootDir}/out",
          featureFlags = FeatureFlagsProvider.getFeatureFlags(),
        ),
    )

  private suspend fun connectBuiltIn(connection: GenericConnection) {
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
      server =
        connection.server.also {
          it.buildInitialize(params = InitializeBuildParams())
          it.onBuildInitialized()
        }
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
    connect()
    val server = server ?: error("Cannot execute the task. Server not available.")
    return task(server)
  }

  override suspend fun disconnect() {}

  override fun isConnected(): Boolean = server != null
}
