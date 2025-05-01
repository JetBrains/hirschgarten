package org.jetbrains.bazel.settings

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.action.getPsiFile
import org.jetbrains.bazel.action.registered.ResyncAction
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings

internal class LoadProjectViewFileAction :
  SuspendableAction(
    {
      BazelPluginBundle.message("action.load.project.view.file")
    },
  ),
  DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val projectViewFile = e.getPsiFile()?.virtualFile ?: return
    project.bazelProjectSettings = project.bazelProjectSettings.withNewProjectViewPath(projectViewFile.toNioPath().toAbsolutePath())
    withContext(Dispatchers.EDT) {
      ActionUtil.performAction(ResyncAction(), e)
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = shouldShowAction(project, e)
  }

  private fun shouldShowAction(project: Project, e: AnActionEvent): Boolean {
    val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext) ?: return false
    return when {
      !project.isBazelProject -> false
      psiFile.isProjectViewFile() &&
        psiFile.isDifferentProjectViewFileSelected() -> true

      else -> false
    }
  }

  private fun PsiFile.isProjectViewFile(): Boolean = virtualFile?.extension == Constants.PROJECT_VIEW_FILE_EXTENSION

  private fun PsiFile.isDifferentProjectViewFileSelected(): Boolean =
    virtualFile?.toNioPath()?.toAbsolutePath() != project.bazelProjectSettings.projectViewPath
}
