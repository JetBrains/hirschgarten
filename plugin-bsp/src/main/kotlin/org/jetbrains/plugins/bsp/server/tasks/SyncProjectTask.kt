package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspToolWindowService

private const val SYNC_TASK_ID = "bsp-sync-project"
private const val RESYNC_TASK_ID = "bsp-resync-project"

private val log = logger<SyncProjectTask>()

public class SyncProjectTask(project: Project) : BspServerTask<Unit>("Sync Project", project) {
  private val connectionService = BspConnectionService.getInstance(project)
  private val syncConsole = BspConsoleService.getInstance(project).bspSyncConsole

  public suspend fun execute(shouldBuildProject: Boolean, shouldReloadConnection: Boolean) {
    saveAllFiles()
    if (shouldReloadConnection) {
      reloadConnection()
    }

    collectProject(SYNC_TASK_ID)

    if (shouldBuildProject) {
      buildProject()
      collectProject(RESYNC_TASK_ID)
    }
  }

  private fun reloadConnection() {
    val connection = connectionService.value
    connection?.disconnect()
    connection?.reload()
  }

  private suspend fun collectProject(taskId: String) {
    val collectProjectDetailsTask = CollectProjectDetailsTask(project, taskId)

    syncConsole.startTask(
      taskId = taskId,
      title = "Sync",
      message = "Syncing...",
      cancelAction = { collectProjectDetailsTask.cancelExecution() },
    )
    connectionService.value!!.connect(taskId) { collectProjectDetailsTask.cancelExecution() }
    try {
      collectProjectDetailsTask.execute(
        name = "Syncing...",
        cancelable = true,
      )
      syncConsole.finishTask(taskId, "Sync")
    } catch (e: Exception) {
      syncConsole.finishTask(taskId, "Sync failed!", FailureResultImpl(e))
    }

    BspToolWindowService.getInstance(project).doDeepPanelReload()
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
