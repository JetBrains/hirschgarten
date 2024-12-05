package org.jetbrains.plugins.bsp.ui.projectTree.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.virtualFile
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.runnerAction.TestTargetAction
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

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
        readAction { getAllTestTargetInfos(project, e) },
        project = project,
      )
    action.actionPerformed(e)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = runReadAction { shouldShowAction(project, e) }
  }

  private fun getAllTestTargetInfos(project: Project, e: AnActionEvent): List<BuildTargetInfo> {
    val currentPath = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return listOf()
    val targetUtilService = project.temporaryTargetUtils
    val pfIndex = ProjectFileIndex.getInstance(project)
    val vfsManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    return currentPath
      .toVirtualFileUrl(vfsManager)
      .subTreeFileUrls
      .asSequence()
      .mapNotNull { it.virtualFile }
      .filter { pfIndex.isInTestSourceContent(it) }
      .flatMap { targetUtilService.getExecutableTargetsForFile(it, project) }
      .distinct()
      .mapNotNull { targetUtilService.getBuildTargetInfoForId(it) }
      .filter { it.capabilities.canTest }
      .toList()
  }

  private fun shouldShowAction(project: Project, e: AnActionEvent): Boolean {
    val currentPath = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return false
    val pfIndex = ProjectFileIndex.getInstance(project)
    val targetUtilService = project.temporaryTargetUtils
    val vfsManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    val subFiles =
      currentPath
        .toVirtualFileUrl(vfsManager)
        .subTreeFileUrls
        .asSequence()
        .mapNotNull { it.virtualFile }
        .filter { pfIndex.isInTestSourceContent(it) }

    for (file in subFiles) {
      val targets =
        targetUtilService
          .getExecutableTargetsForFile(file, project)
          .mapNotNull { targetUtilService.getBuildTargetInfoForId(it) }
          .filter { it.capabilities.canTest }
      if (targets.isNotEmpty()) {
        return true
      }
    }
    return false
  }
}
