package org.jetbrains.bazel.flow.open.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.flow.open.BazelApplicationCoroutineScopeService
import org.jetbrains.bazel.flow.open.BazelOpenProjectProvider
import org.jetbrains.bazel.flow.open.BazelUnlinkedProjectAware.Companion.closeAndReopenAsBazelProject
import org.jetbrains.bazel.flow.open.findProjectFolderFromVFile

internal class LinkBazelProjectFromScriptAction :
  DumbAwareAction(
    { BazelPluginBundle.message("action.link.bazel.project.from.linked.project") },
    BazelPluginIcons.bazel,
  ) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val filePath = virtualFile.toNioPath()
    val service = ApplicationManager.getApplication().service<BazelApplicationCoroutineScopeService>()

    service.launch {
      val projectFolder = withContext(Dispatchers.IO) {
        findProjectFolderFromVFile(virtualFile)
      }
      if (projectFolder == null) return@launch
      closeAndReopenAsBazelProject(project, filePath)
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
