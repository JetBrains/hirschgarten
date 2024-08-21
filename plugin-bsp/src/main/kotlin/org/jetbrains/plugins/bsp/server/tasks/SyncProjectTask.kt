package org.jetbrains.plugins.bsp.server.tasks

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspSyncStatusService
import org.jetbrains.plugins.bsp.config.SyncAlreadyInProgressException
import org.jetbrains.plugins.bsp.performance.testing.bspTracer
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowService
import java.util.concurrent.CancellationException

private const val SYNC_TASK_ID = "bsp-sync-project"

private val log = logger<SyncProjectTask>()

public class SyncProjectTask(project: Project) : BspServerTask<Unit>("Sync Project", project) {
  public suspend fun execute(shouldBuildProject: Boolean) {
    if (BspSyncStatusService.getInstance(project).isSyncInProgress) return

    bspTracer.spanBuilder("bsp.sync.project.ms").useWithScope {
      var syncAlreadyInProgress = false
      try {
        log.debug("Starting sync project task")
        preSync()
        collectProject(SYNC_TASK_ID, shouldBuildProject)
      } catch (_: SyncAlreadyInProgressException) {
        syncAlreadyInProgress = true
      } finally {
        if (syncAlreadyInProgress) return@useWithScope
        BspSyncStatusService.getInstance(project).finishSync()
        ProjectView.getInstance(project).refresh()
      }
    }
  }

  private suspend fun preSync() {
    log.debug("Running pre sync tasks")
    BspSyncStatusService.getInstance(project).startSync()
    saveAllFiles()
  }

  private suspend fun collectProject(taskId: String, buildProject: Boolean) =
    coroutineScope {
      val syncConsole = BspConsoleService.getInstance(project).bspSyncConsole
      try {
        log.debug("Collecting project details")
        val collectProjectDetailsTask =
          CollectProjectDetailsTask(
            project = project,
            taskId = taskId,
            name = "Syncing...",
            cancelable = true,
            buildProject = buildProject,
          )
        syncConsole.startTask(
          taskId = taskId,
          title = BspPluginBundle.message("console.task.sync.title"),
          message = BspPluginBundle.message("console.task.sync.in.progress"),
          cancelAction = {
            BspSyncStatusService.getInstance(project).cancel()
            coroutineContext.cancel()
          },
        )
        log.debug("Connecting to the server")
        project.connection.connect(taskId)
        log.debug("Running CollectProjectDetailsTask")
        collectProjectDetailsTask.execute()
        syncConsole.finishTask(taskId, BspPluginBundle.message("console.task.sync.success"))
      } catch (e: Exception) {
        if (e is CancellationException) {
          syncConsole.finishTask(taskId, BspPluginBundle.message("console.task.sync.cancelled"), FailureResultImpl())
        } else {
          log.debug("BSP sync failed")
          syncConsole.finishTask(taskId, BspPluginBundle.message("console.task.sync.failed"), FailureResultImpl(e))
        }
      }

      BspToolWindowService.getInstance(project).doDeepPanelReload()
    }
}
