package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings

@ApiStatus.Internal
class DefaultProjectViewService(private val project: Project) : ProjectViewService {

  private val _projectViewState = MutableStateFlow(ProjectView.EMPTY)

  override val allowExternalProjectViewModification: Boolean = true

  override val projectViewState: StateFlow<ProjectView>
    get() = _projectViewState

  private suspend fun computeProjectView(): ProjectView {
    var projectViewPath = findProjectViewPath()

    if (projectViewPath == null || !projectViewPath.exists()) {
      val newProjectView = ProjectViewFileUtils.calculateProjectViewFilePath(
        overwrite = false,
        projectRootDir = project.rootDir,
        projectViewPath = null,
      )
      projectViewPath = VirtualFileManager.getInstance()
        .findFileByNioPath(newProjectView)
      project.bazelProjectSettings = project.bazelProjectSettings.withNewProjectViewPath(projectViewPath)
    }

    if (projectViewPath == null) {
      return getDefaultProjectView()
    }

    return parseProjectView(projectViewPath)
  }

  private fun findProjectViewPath(): VirtualFile? {
    val pathFromSettings = project.bazelProjectSettings.projectViewPath
    if (pathFromSettings != null && pathFromSettings.exists()) {
      return pathFromSettings
    }
    val rootDir = project.rootDir
    return rootDir.findFile(".bazelproject")
           ?: rootDir.findFileByRelativePath("${Constants.DOT_BAZELBSP_DIR_NAME}/.bazelproject")
  }

  private fun parseProjectView(projectViewPath: VirtualFile): ProjectView {
    return runBlockingMaybeCancellable {
      parseProjectViewAsync(projectViewPath) ?: getDefaultProjectView()
    }
  }

  private suspend fun parseProjectViewAsync(projectViewPath: VirtualFile): ProjectView? {
    return readAction {
      val psi = PsiManager.getInstance(project).findFile(projectViewPath) as? ProjectViewPsiFile
                ?: return@readAction null
      ProjectView.fromProjectViewPsiFile(psi)
    }
  }

  private suspend fun getDefaultProjectView(): ProjectView {
    return readAction {
      val content = ProjectViewFileUtils.projectViewTemplate(project.rootDir)
      val psiFile = PsiFileFactory.getInstance(project)
        .createFileFromText(".bazelproject", ProjectViewLanguage, content) as ProjectViewPsiFile
      ProjectView.fromProjectViewPsiFile(psiFile)
    }
  }

  suspend fun ensureProjectViewInitialized() {
    if (_projectViewState.value === ProjectView.EMPTY) {
      val newProjectView = computeProjectView()
      _projectViewState.emit(newProjectView)
    }
  }

  override suspend fun forceReparseCurrentProjectViewFiles() {
    val newProjectView = computeProjectView()
    _projectViewState.emit(newProjectView)

    val projectViewPath = findProjectViewPath() ?: return
    val imports = parseProjectViewAsync(projectViewPath)?.imports ?: emptyList()

    writeAction {
      PsiDocumentManager.getInstance(project)
        .reparseFiles(imports + projectViewPath, false)
    }
  }
}
