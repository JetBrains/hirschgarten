package org.jetbrains.bazel.flow.open.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.flow.open.BazelBspOpenProjectProvider
import org.jetbrains.bazel.flow.open.BazelBspProjectOpenProcessor

internal class LinkBazelProjectFromScriptAction :
  DumbAwareAction(
    { BazelPluginBundle.message("action.link.bazel.project.from.linked.project") },
    BazelPluginIcons.bazel,
  ) {
  override fun actionPerformed(e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val projectFile = BazelBspProjectOpenProcessor().calculateProjectFolderToOpen(virtualFile)
    val project = e.project ?: return

    BspCoroutineService.getInstance(project).start {
      BazelBspOpenProjectProvider().linkToExistingProjectAsync(projectFile, project)
    }
  }

  override fun update(e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = virtualFile.isFileSupported() && !project.isBspProject
  }

  private fun VirtualFile.isFileSupported() =
    BazelBspOpenProjectProvider().isProjectFile(this) ||
      extension == BazelPluginConstants.PROJECT_VIEW_FILE_EXTENSION

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
