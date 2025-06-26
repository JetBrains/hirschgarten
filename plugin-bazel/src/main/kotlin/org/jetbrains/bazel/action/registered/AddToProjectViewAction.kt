package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.nio.file.Path
import java.nio.file.Paths

class AddToProjectViewAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedDirectoryPath = getPathOfSelectedDirectory(e) ?: return
    val selectedDirectoryRelativePath = getPathRelativeToProjectRoot(project, selectedDirectoryPath) ?: return
    val selectedDirectoryPsi = createDirectoriesPsi(project, selectedDirectoryRelativePath)
    val directoriesPsi = getProjectViewPsiFile(project)?.getSection("directories") ?: return
    val colonPsi = selectedDirectoryPsi.getColon() ?: return
    WriteCommandAction.runWriteCommandAction(project) {
      directoriesPsi.addRangeAfter(colonPsi.nextSibling, selectedDirectoryPsi.lastChild, directoriesPsi.getItems().last())
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = getProjectViewPath(project) != null && getPathOfSelectedDirectory(e) != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun getProjectViewPath(project: Project): Path? = project.bazelProjectSettings.projectViewPath

  private fun getPathOfSelectedDirectory(e: AnActionEvent): Path? =
    e.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf { it.isDirectory }?.path?.let { path ->
      Paths.get(path)
    }

  private fun getPathRelativeToProjectRoot(project: Project, path: Path): Path? {
    val basePath = project.basePath ?: return null
    return Paths.get(basePath).relativize(path)
  }

  private fun getProjectViewPsiFile(project: Project): ProjectViewPsiFile? {
    val projectViewPath = getProjectViewPath(project) ?: return null
    val vFile = LocalFileSystem.getInstance().findFileByNioFile(projectViewPath) ?: return null
    return PsiManager.getInstance(project).findFile(vFile) as? ProjectViewPsiFile
  }

  private fun createDirectoriesPsi(project: Project, directory: Path): ProjectViewPsiSection =
    PsiFileFactory
      .getInstance(project)
      .createFileFromText(
        "dummy.bazelproject",
        ProjectViewFileType,
        "directories:\n  $directory",
      ).getChildrenOfType<ProjectViewPsiSection>()
      .first()
}
