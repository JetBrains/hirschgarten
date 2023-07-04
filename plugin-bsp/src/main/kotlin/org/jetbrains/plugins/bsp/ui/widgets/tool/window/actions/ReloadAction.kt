package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.server.connection.BspConnection
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspFileConnection
import org.jetbrains.plugins.bsp.server.connection.BspGeneratorConnection
import org.jetbrains.plugins.bsp.server.tasks.CollectProjectDetailsTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

private const val RELOAD_TASK_ID = "bsp-reload"

public class ReloadAction :
  AnAction({ BspAllTargetsWidgetBundle.message("reload.action.text") }, BspPluginIcons.reload) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      BspCoroutineService.getInstance(project).start { doAction(project) }
    } else {
      log.warn("ReloadAction cannot be performed! Project not available.")
    }
  }

  private suspend fun doAction(project: Project) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val collectProjectDetailsTask = CollectProjectDetailsTask(project, RELOAD_TASK_ID)

    bspSyncConsole.startTask(
      taskId = RELOAD_TASK_ID,
      title = "Reload",
      message = "Reloading...",
      cancelAction = { collectProjectDetailsTask.cancelExecution() }
    )

    try {
      val connection = BspConnectionService.getInstance(project).value!!
      connection.handleConnection(project) { collectProjectDetailsTask.cancelExecution() }
      collectProjectDetailsTask.execute(name = "Reloading...", cancelable = true)
      bspSyncConsole.finishTask(RELOAD_TASK_ID, "Reload done!")
    } catch (e: Exception) {
      bspSyncConsole.finishTask(RELOAD_TASK_ID, "Reload failed!", FailureResultImpl(e))
    }
  }

  private fun BspConnection.handleConnection(project: Project, errorCallback: () -> Unit) = when (this) {
    is BspFileConnection -> {
      this.disconnect()
      val locatedBspConnectionDetails =
        LocatedBspConnectionDetailsParser.parseFromFile(this.locatedConnectionFile.connectionFileLocation)
      doReloadConnectionFile(project, locatedBspConnectionDetails, errorCallback)
    }

    is BspGeneratorConnection ->
      this.getLocatedBspConnectionDetails()?.let {
        this.disconnect()
        doReloadConnectionFile(project, it, errorCallback)
      }

    else -> {}
  }

  private fun doReloadConnectionFile(project: Project,
                                     locatedBspConnectionDetails: LocatedBspConnectionDetails,
                                     errorCallback: () -> Unit
  ) {
    val newBspFileConnection = BspFileConnection(project, locatedBspConnectionDetails)
    newBspFileConnection.connect(RELOAD_TASK_ID, errorCallback)
    val bspConnectionService = BspConnectionService.getInstance(project)
    bspConnectionService.value = newBspFileConnection
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doUpdate(e, project)
    } else {
      log.warn("ReloadAction cannot be updated! Project not available.")
    }
  }

  private fun doUpdate(e: AnActionEvent, project: Project) {
    val connection = BspConnectionService.getInstance(project).value
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    e.presentation.isEnabled = connection != null && !bspSyncConsole.hasTasksInProgress()
  }

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.BGT

  private companion object {
    private val log = logger<ReloadAction>()
  }
}
