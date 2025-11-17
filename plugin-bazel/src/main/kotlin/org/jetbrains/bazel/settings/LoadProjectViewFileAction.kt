package org.jetbrains.bazel.settings

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.action.registered.ResyncAction
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings

internal class LoadProjectViewFileAction :
  SuspendableAction(
    {
      BazelPluginBundle.message("action.load.project.view.file")
    },
  ),
  DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    project.bazelProjectSettings = project.bazelProjectSettings
      .withNewProjectViewPath(e.getData(CommonDataKeys.VIRTUAL_FILE))

    withContext(Dispatchers.EDT) {
      ActionUtil.performAction(ResyncAction(), e)
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

    e.presentation.isEnabledAndVisible =
      virtualFile != null &&
        virtualFile.extension == Constants.PROJECT_VIEW_FILE_EXTENSION &&
        virtualFile.toNioPath().toAbsolutePath() != project.bazelProjectSettings.projectViewPath
  }
}
