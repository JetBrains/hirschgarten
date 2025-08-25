package org.jetbrains.bazel.action.registered

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import java.nio.file.Path

class OpenProjectViewAction :
  SuspendableAction(
    { BazelPluginBundle.message("widget.config.file.popup.message", BazelPluginBundle.message("tool.window.generic.config.file")) },
    AllIcons.FileTypes.Config,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    openProjectView(project)
  }
}

internal suspend fun openProjectView(project: Project) {
  val configFile = project.bazelProjectSettings.projectViewPath
  withContext(Dispatchers.EDT) {
    project.serviceAsync<ProjectView>().refresh()
    if (configFile != null) {
      getPsiFile(configFile, project)?.navigate(true)
    }
  }
}

private suspend fun getPsiFile(file: Path, project: Project): PsiFile? {
  val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(file) ?: return null
  return project.serviceAsync<PsiManager>().findFile(virtualFile)
}
