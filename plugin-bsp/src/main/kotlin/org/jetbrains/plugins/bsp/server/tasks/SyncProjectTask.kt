package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspSyncStatusService
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowService

private const val SYNC_TASK_ID = "bsp-sync-project"
private const val RESYNC_TASK_ID = "bsp-resync-project"

private val log = logger<SyncProjectTask>()

public class SyncProjectTask(project: Project) : BspServerTask<Unit>("Sync Project", project) {
  public suspend fun execute(
    shouldRunInitialSync: Boolean,
    shouldBuildProject: Boolean,
    shouldRunResync: Boolean,
  ) {
    try {
      preSync()
      if (shouldRunInitialSync) {
        collectProject(SYNC_TASK_ID)
      }

      if (shouldBuildProject) {
        buildProject()
      }

      if (shouldRunResync) {
        collectProject(RESYNC_TASK_ID)
      }
    } finally {
      BspSyncStatusService.getInstance(project).finishSync()
    }
  }

  private fun preSync() {
    BspSyncStatusService.getInstance(project).startSync()
    saveAllFiles()
  }

  private suspend fun collectProject(taskId: String) {
    val collectProjectDetailsTask = CollectProjectDetailsTask(project, taskId)

    val syncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    syncConsole.startTask(
      taskId = taskId,
      title = BspPluginBundle.message("console.task.sync.title"),
      message = BspPluginBundle.message("console.task.sync.in.progress"),
      cancelAction = { collectProjectDetailsTask.onCancel() },
    )
    project.connection.connect(taskId) { collectProjectDetailsTask.cancelExecution() }
    try {
      collectProjectDetailsTask.execute(
        name = "Syncing...",
        cancelable = true,
      )
      syncConsole.finishTask(taskId, BspPluginBundle.message("console.task.sync.success"))
    } catch (e: Exception) {
      syncConsole.finishTask(taskId, BspPluginBundle.message("console.task.sync.failed"), FailureResultImpl(e))
    }

    BspToolWindowService.getInstance(project).doDeepPanelReload()
  }

  private fun CollectProjectDetailsTask.onCancel() {
    BspSyncStatusService.getInstance(project).cancel()
    this.cancelExecution()
  }

  private suspend fun buildProject() {
    val targetsToBuild = calculateAllTargetsToBuild()
    runBuildTargetTask(targetsToBuild, project, log)
  }

  private fun calculateAllTargetsToBuild(): List<BuildTargetIdentifier> {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    val allTargets = magicMetaModel.getAllLoadedTargets() + magicMetaModel.getAllNotLoadedTargets()
    val compilableTargets = allTargets.filter { it.capabilities.canCompile }

    return compilableTargets.map { it.id.toBsp4JTargetIdentifier() }
  }
}
