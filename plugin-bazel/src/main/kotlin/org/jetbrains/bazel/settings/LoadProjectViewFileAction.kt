package org.jetbrains.bazel.settings

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.impl.actions.registered.ResyncAction

internal class LoadProjectViewFileAction :
  SuspendableAction({
    BazelPluginBundle.message("action.load.project.view.file")
  }),
  DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val projectViewFile = CommonDataKeys.PSI_FILE.getData(e.dataContext)?.virtualFile ?: return
    project.bazelProjectSettings = project.bazelProjectSettings.withNewProjectViewPath(projectViewFile.toNioPath().toAbsolutePath())
    ResyncAction().actionPerformed(e)
  }

  override fun update(project: Project, e: AnActionEvent) {
    if (!project.isBazelProject) return
    val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext) ?: return

    e.presentation.isEnabledAndVisible = psiFile.isProjectViewFile() && psiFile.isDifferentProjectViewFileSelected()
  }

  private fun PsiFile.isProjectViewFile(): Boolean = virtualFile?.extension == BazelPluginConstants.PROJECT_VIEW_FILE_EXTENSION

  private fun PsiFile.isDifferentProjectViewFileSelected(): Boolean =
    virtualFile?.toNioPath()?.toAbsolutePath() != project.bazelProjectSettings.projectViewPath
}
