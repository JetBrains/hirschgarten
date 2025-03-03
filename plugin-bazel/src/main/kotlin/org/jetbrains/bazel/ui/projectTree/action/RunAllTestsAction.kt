package org.jetbrains.bazel.ui.projectTree.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.virtualFile
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.runnerAction.RunWithCoverageAction
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon

internal open class RunAllTestsBaseAction(
  text: () -> String,
  icon: Icon,
  private val createAction: (targets: List<BuildTargetInfo>, directoryName: String) -> SuspendableAction,
) : SuspendableAction(
    text = text,
    icon = icon,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val action =
      createAction(
        readAction { getAllTestTargetInfos(project, e) },
        e.getCurrentPath()?.name.orEmpty(),
      )
    action.actionPerformed(e)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = runReadAction { shouldShowAction(project, e) }
  }

  private fun getAllTestTargetInfos(project: Project, e: AnActionEvent): List<BuildTargetInfo> =
    e
      .getCurrentPath()
      ?.toSubFilesInTestSourceContent(project)
      ?.filter { it.capabilities.canTest }
      ?.toList() ?: listOf()

  private fun shouldShowAction(project: Project, e: AnActionEvent): Boolean {
    return e.getCurrentPath()?.toSubFilesInTestSourceContent(project)?.any { it.capabilities.canTest } ?: return false
  }

  private fun AnActionEvent.getCurrentPath(): VirtualFile? = getData(PlatformDataKeys.VIRTUAL_FILE)

  private fun VirtualFile.toSubFilesInTestSourceContent(project: Project): Sequence<BuildTargetInfo> {
    val pfIndex = ProjectFileIndex.getInstance(project)
    val vfsManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    val targetUtilService = project.targetUtils

    return this
      .toVirtualFileUrl(vfsManager)
      .subTreeFileUrls
      .asSequence()
      .mapNotNull { it.virtualFile }
      .filter { pfIndex.isInTestSourceContent(it) }
      .flatMap { targetUtilService.getExecutableTargetsForFile(it) }
      .distinct()
      .mapNotNull { targetUtilService.getBuildTargetInfoForLabel(it) }
  }
}

internal class RunAllTestsAction :
  RunAllTestsBaseAction(
    text = { BspPluginBundle.message("action.run.all.tests") },
    icon = AllIcons.Actions.Execute,
    createAction = { targets, directoryName ->
      TestTargetAction(targets, text = {
        BspPluginBundle.message("action.run.all.tests.under", directoryName)
      })
    },
  )

internal class RunAllTestsWithCoverageAction :
  RunAllTestsBaseAction(
    text = { BspPluginBundle.message("action.run.all.tests.with.coverage") },
    icon = AllIcons.General.RunWithCoverage,
    createAction = { targets, directoryName ->
      RunWithCoverageAction(targets, text = {
        BspPluginBundle.message("action.run.all.tests.under.with.coverage", directoryName)
      })
    },
  )
