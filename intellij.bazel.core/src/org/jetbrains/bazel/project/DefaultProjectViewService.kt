package org.jetbrains.bazel.project

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.stateStore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.imports.resolvedFileOrNull
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.utils.findVirtualFile
import java.nio.file.Path

@ApiStatus.Internal
class DefaultProjectViewService(private val project: Project) : ProjectViewService {
  val projectViewFile: VirtualFile by lazy {
    findProjectViewVirtualFile()
  }

  override val projectViewState: StateFlow<ProjectView>
    field = MutableStateFlow(ProjectView.EMPTY)

  private suspend fun computeProjectView(): ProjectView {
    return parseProjectViewAsync(projectViewFile) ?: getDefaultProjectView()
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
        ProjectView.fromProjectViewContent(project, content)
    }
  }

  suspend fun ensureProjectViewInitialized() {
    if (projectViewState.value === ProjectView.EMPTY) {
      val newProjectView = computeProjectView()
      projectViewState.emit(newProjectView)
    }
  }

  override suspend fun forceReparseCurrentProjectViewFiles() {
    logger.info("Reparsing currently opened project view file")
    val newProjectView = computeProjectView()
    projectViewState.emit(newProjectView)
    val resolvedImports = newProjectView.imports.mapNotNull { it.resolvedFileOrNull() }
    edtWriteAction {
      PsiDocumentManager.getInstance(project)
        .reparseFiles(resolvedImports + projectViewFile, false)
    }
  }

  private fun findProjectViewVirtualFile(): VirtualFile =
    getProjectViewFilePath(project)?.findVirtualFile() ?: error("Could not find project view file in VFS")

  // Only to be used in monorepo devkit module
  suspend fun forceLoadProjectViewFile(newFile: VirtualFile) {
    logger.info("Forcibly loading project view file content: $newFile")
    val newProjectView = parseProjectViewAsync(newFile) ?: return
    projectViewState.emit(newProjectView)
  }

  companion object {
    private val logger = logger<DefaultProjectViewService>()

    @JvmStatic
    fun getInstance(project: Project): DefaultProjectViewService = ProjectViewService.getInstance(project) as DefaultProjectViewService

    fun getProjectViewFilePath(project: Project): Path? =
      (project.stateStore.storeDescriptor as? BazelProjectStoreDescriptor)?.projectViewFile
  }
}

internal val Project.projectViewFile: VirtualFile
  get() = DefaultProjectViewService.getInstance(this).projectViewFile

internal val Project.projectViewFilePsiFile: ProjectViewPsiFile?
  get() = PsiManager.getInstance(this).findFile(projectViewFile) as? ProjectViewPsiFile
