package org.jetbrains.plugins.bsp.server.tasks

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspSyncStatusService
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowService

private const val SYNC_TASK_ID = "bsp-sync-project"

private val log = logger<SyncProjectTask>()

public class SyncProjectTask(project: Project) : BspServerTask<Unit>("Sync Project", project) {
  public suspend fun execute(shouldBuildProject: Boolean) {
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

  private suspend fun collectProject(taskId: String, buildProject: Boolean) {
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
    project.connection.connect(taskId) { collectProjectDetailsTask.cancelExecution() }
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
