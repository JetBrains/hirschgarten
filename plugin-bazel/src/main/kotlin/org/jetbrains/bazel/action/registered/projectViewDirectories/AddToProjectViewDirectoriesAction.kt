package org.jetbrains.bazel.action.registered.projectViewDirectories

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.bazel.config.BazelPluginBundle.message
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.addDirectoriesInclude
import org.jetbrains.bazel.languages.projectview.psi.directoriesContainsInclude
import org.jetbrains.bazel.languages.projectview.psi.getProjectViewPsiFileOrNull
import org.jetbrains.bazel.languages.projectview.psi.isDirectoriesNullOrEmpty
import org.jetbrains.bazel.languages.projectview.psi.removeDirectoriesExclude
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.utils.findNearestParent
import org.jetbrains.bazel.utils.selectedDirectory
import org.jetbrains.bazel.workspace.excludedRoots
import org.jetbrains.bazel.workspace.includedRoots

class AddToProjectViewDirectoriesAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val directory = e.selectedDirectory ?: return
    val project = e.project ?: return
    val projectViewPsi = project.getProjectViewPsiFileOrNull() ?: return
    val includes = project.includedRoots().orEmpty()
    val excludes = project.excludedRoots().orEmpty()
    val nearestParent = directory.findNearestParent(includes + excludes - directory)
    val isNotIncluded = nearestParent !in includes
    val isExplicitlyExcluded = directory in excludes
    if (!isExplicitlyExcluded && !isNotIncluded) return
    projectViewPsi.navigate(/* requestFocus = */ true)
    if (projectViewPsi.directoriesContainsInclude(directory)) return
    WriteCommandAction.writeCommandAction(project, projectViewPsi).compute<Unit, Throwable> {
      if (projectViewPsi.isDirectoriesNullOrEmpty()) {
        projectViewPsi.addDirectoriesInclude(project.rootDir)
      }
      if (isExplicitlyExcluded) {
        projectViewPsi.removeDirectoriesExclude(directory)
      }
      if (isNotIncluded) {
        projectViewPsi.addDirectoriesInclude(directory)
      }
      PsiDocumentManager
        .getInstance(project)
        .reparseFiles(/* files = */ listOf(projectViewPsi.virtualFile), /* includeOpenFiles = */ false)
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val projectViewFile = project?.getProjectViewIfApplicableTo(e)
    val rootDir = project?.bazelProjectProperties?.rootDir
    if (projectViewFile == null || rootDir == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = true
    e.presentation.text = message("action.Bazel.AddToProjectViewDirectoriesAction.text", projectViewFile.name)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun Project.getProjectViewIfApplicableTo(e: AnActionEvent): VirtualFile? {
    if (DumbService.isDumb(this)) return null
    val directory = e.selectedDirectory ?: return null
    val projectViewFile = bazelProjectSettings.projectViewPath ?: return null
    val includes = includedRoots().orEmpty()
    val excludes = excludedRoots().orEmpty()
    return when (directory) {
      in includes -> null
      in excludes -> projectViewFile
      else -> when (directory.findNearestParent(includes + excludes)) {
        in includes -> null
        else -> projectViewFile
      }
    }
  }
}
