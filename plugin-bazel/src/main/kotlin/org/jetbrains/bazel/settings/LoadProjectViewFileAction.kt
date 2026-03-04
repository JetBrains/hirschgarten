package org.jetbrains.bazel.settings

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.projectViewFile

internal class LoadProjectViewFileAction :
  SuspendableAction(
    {
      BazelPluginBundle.message("action.load.project.view.file")
    },
  ),
  DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectUtil.openOrImport(e.getData(CommonDataKeys.VIRTUAL_FILE)!!.toNioPath())
  }

  override fun update(project: Project, e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

    e.presentation.isEnabledAndVisible =
      virtualFile != null &&
        virtualFile.extension == Constants.PROJECT_VIEW_FILE_EXTENSION &&
        virtualFile.toNioPath().toAbsolutePath() != project.projectViewFile?.toNioPath()?.toAbsolutePath()
  }
}
