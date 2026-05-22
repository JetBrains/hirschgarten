package org.jetbrains.bazel.flow.open.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService
import org.jetbrains.bazel.flow.open.closeAndReopenAsBazelProject
import org.jetbrains.bazel.flow.open.findProjectFolderFromVFile
import org.jetbrains.bazel.flow.open.isBazelWorkspaceFile

internal class LinkBazelProjectFromScriptAction :
  DumbAwareAction(
    { BazelPluginBundle.message("action.link.bazel.project.from.linked.project") },
    BazelPluginIcons.bazel,
  ) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val filePath = virtualFile.toNioPath()

    BazelApplicationCoroutineScopeService.getInstance().launch {
      val projectFolder = withContext(Dispatchers.IO) {
        findProjectFolderFromVFile(virtualFile)
      }
      if (projectFolder == null) return@launch
      closeAndReopenAsBazelProject(project, filePath)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project?.isBazelProject != true &&
      e.getData(CommonDataKeys.VIRTUAL_FILE)?.isBazelWorkspaceFile() == true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
