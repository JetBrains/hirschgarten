package org.jetbrains.bazel.flow.open.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.flow.open.BazelOpenProjectProvider
import org.jetbrains.bazel.flow.open.BazelProjectOpenProcessor

internal class LinkBazelProjectFromScriptAction :
  DumbAwareAction(
    { BazelPluginBundle.message("action.link.bazel.project.from.linked.project") },
    BazelPluginIcons.bazel,
  ) {
  override fun actionPerformed(e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val projectFile = BazelProjectOpenProcessor().calculateProjectFolderToOpen(virtualFile)
    val project = e.project ?: return

    BazelCoroutineService.getInstance(project).start {
      BazelOpenProjectProvider().linkToExistingProjectAsync(projectFile, project)
    }
  }

  override fun update(e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = virtualFile.isFileSupported() && !project.isBazelProject
  }

  private fun VirtualFile.isFileSupported() =
    BazelOpenProjectProvider().isProjectFile(this) ||
      extension == Constants.PROJECT_VIEW_FILE_EXTENSION

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
