package org.jetbrains.bazel.flow.open.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.launch
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.flow.open.BazelOpenProjectProvider
import org.jetbrains.bazel.flow.open.findProjectFolderFromVFile

internal class LinkBazelProjectFromScriptAction :
  DumbAwareAction(
    { BazelPluginBundle.message("action.link.bazel.project.from.linked.project") },
    BazelPluginIcons.bazel,
  ) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val projectFile = findProjectFolderFromVFile(virtualFile) ?: return

    e.coroutineScope.launch {
      BazelOpenProjectProvider().linkToExistingProjectAsync(projectFile, project)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project?.isBazelProject != true &&
      e.getData(CommonDataKeys.VIRTUAL_FILE)?.isFileSupported() == true
  }

  private fun VirtualFile.isFileSupported() =
    BazelOpenProjectProvider().isProjectFile(this) ||
      extension == Constants.PROJECT_VIEW_FILE_EXTENSION

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
