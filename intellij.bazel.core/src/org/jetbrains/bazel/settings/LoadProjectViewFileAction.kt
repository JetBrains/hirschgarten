package org.jetbrains.bazel.settings

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService
import org.jetbrains.bazel.flow.open.closeAndReopenAsBazelProject
import org.jetbrains.bazel.project.projectViewFile

internal class LoadProjectViewFileAction :
  SuspendableAction(
    {
      BazelPluginBundle.message("action.load.project.view.file")
    },
  ),
  DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val projectViewFile = e.getData(CommonDataKeys.VIRTUAL_FILE)!!.toNioPath()
    BazelApplicationCoroutineScopeService.getInstance().launch {
      closeAndReopenAsBazelProject(project, projectViewFile)
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

    e.presentation.isEnabledAndVisible =
      virtualFile != null &&
      virtualFile.extension == Constants.PROJECT_VIEW_FILE_EXTENSION &&
      virtualFile != project.projectViewFile
  }
}
