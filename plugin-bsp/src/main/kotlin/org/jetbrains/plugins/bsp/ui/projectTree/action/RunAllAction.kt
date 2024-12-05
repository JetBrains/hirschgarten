package org.jetbrains.plugins.bsp.ui.projectTree.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.runnerAction.TestTargetAction
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.walk

internal class RunAllTestAction :
  SuspendableAction(
    {
      BspPluginBundle.message("action.run.all.test")
    },
    AllIcons.Actions.Execute,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val action =
      TestTargetAction(
        getAllTestTargetInfos(project, e),
        project = project,
      )
    action.actionPerformed(e)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = shouldShowAction(project, e)
  }

  @OptIn(ExperimentalPathApi::class)
  private fun getAllTestTargetInfos(project: Project, e: AnActionEvent): List<BuildTargetInfo> {
    // get current path via vfs
    val currentPath = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return listOf()
    val targetUtilService = project.temporaryTargetUtils
    return currentPath
      .toNioPath()
      .walk()
      .mapNotNull { targetUtilService.fileToTargetId[it.toUri()] }
      .flatten()
      .distinct()
      .mapNotNull { targetUtilService.getBuildTargetInfoForId(it) }
      .filter { it.capabilities.canTest }
      .toList()
  }

  @OptIn(ExperimentalPathApi::class)
  private fun shouldShowAction(project: Project, e: AnActionEvent): Boolean {
    val currentPath = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return false
    val targetUtilService = project.temporaryTargetUtils
    for (path in currentPath.toNioPath().walk()) {
      val targets =
        targetUtilService.fileToTargetId[path.toUri()]
          ?.mapNotNull { targetUtilService.getBuildTargetInfoForId(it) }
          ?.filter { it.capabilities.canTest }
      if (!targets.isNullOrEmpty()) {
        return true
      }
    }
    return false
  }
}
