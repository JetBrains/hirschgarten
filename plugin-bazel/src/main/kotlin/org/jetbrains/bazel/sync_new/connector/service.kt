package org.jetbrains.bazel.sync_new.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.client.BazelClient
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.flow.SyncConsoleTask
import org.jetbrains.bazel.ui.console.ConsoleService
import kotlin.io.path.absolutePathString

@Service(Service.Level.PROJECT)
class BazelConnectorService(private val project: Project) {

  suspend fun ofSyncTask(task: SyncConsoleTask? = null): BazelConnector {
    val consoleService = ConsoleService.getInstance(project)
    val client = BazelClient(consoleService.syncConsole, consoleService.buildConsole, project)
    val context = LegacyBazelFrontendBridge.fetchWorkspaceContext(project)
    return LegacyBazelConnectorImpl(
      project = project,
      executable = context.bazelBinary?.absolutePathString() ?: error("bazel binary not found"),
      bspClient = BspClientLogger(client, task?.parentTaskId),
      coroutineScope = BazelCoroutineService.getInstance(project).coroutineScope,
    )
  }
}
