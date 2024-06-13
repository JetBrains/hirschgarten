package org.jetbrains.plugins.bsp.server.tasks

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.runInterruptible
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspSyncStatusService
import org.jetbrains.plugins.bsp.performance.testing.bspTracer
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowService
import java.util.concurrent.CancellationException

private const val SYNC_TASK_ID = "bsp-sync-project"

private val log = logger<SyncProjectTask>()

public class SyncProjectTask(project: Project) : BspServerTask<Unit>("Sync Project", project) {
  public suspend fun execute(
    shouldBuildProject: Boolean,
  ): Unit = bspTracer.spanBuilder("bsp.sync.project.ms").useWithScope {
    try {
      log.debug("Starting sync project task")
      preSync()
      collectProject(SYNC_TASK_ID, shouldBuildProject)
    } finally {
      BspSyncStatusService.getInstance(project).finishSync()
    }
  }

  private fun preSync() {
    log.debug("Running pre sync tasks")
    BspSyncStatusService.getInstance(project).startSync()
    saveAllFiles()
  }

  private suspend fun collectProject(taskId: String, buildProject: Boolean) = coroutineScope {
    log.debug("Collecting project details")
    val collectProjectDetailsTask = CollectProjectDetailsTask(project, taskId)

    val syncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    syncConsole.startTask(
      taskId = taskId,
      title = BspPluginBundle.message("console.task.sync.title"),
      message = BspPluginBundle.message("console.task.sync.in.progress"),
      cancelAction = { collectProjectDetailsTask.onCancel() },
    )
    log.debug("Connecting to the server")
    runInterruptible {
      project.connection.connect(taskId) { errorMessage ->
        collectProjectDetailsTask.cancelExecution()
        coroutineContext.job.cancel(cause = CancellationException(errorMessage))
      }
    }
    try {
      log.debug("Running CollectProjectDetailsTask")
      collectProjectDetailsTask.execute(
        name = "Syncing...",
        cancelable = true,
        buildProject = buildProject
      )
      syncConsole.finishTask(taskId, BspPluginBundle.message("console.task.sync.success"))
    } catch (e: Exception) {
      log.debug("BSP sync failed")
      syncConsole.finishTask(taskId, BspPluginBundle.message("console.task.sync.failed"), FailureResultImpl(e))
    }

    BspToolWindowService.getInstance(project).doDeepPanelReload()
  }

  private fun CollectProjectDetailsTask.onCancel() {
    log.debug("Cancelling BSP sync")
    BspSyncStatusService.getInstance(project).cancel()
    this.cancelExecution()
  }
}
