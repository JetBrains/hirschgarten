package org.jetbrains.bazel.project

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.ProjectViewServiceBase
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.sync.environment.stateStoreOrNull
import org.jetbrains.bazel.utils.findVirtualFile
import java.nio.file.Path

@ApiStatus.Internal
class DefaultProjectViewService(project: Project) : ProjectViewServiceBase(project) {

  val projectViewFile: VirtualFile by lazy {
    requireProjectViewPath().findVirtualFile() ?: error("Could not find project view file in VFS")
  }

  override val projectViewPath: Path?
    get() = (project.stateStoreOrNull?.storeDescriptor as? BazelProjectStoreDescriptor)?.projectViewFile

  fun ensureProjectViewInitialized() {
    if (projectView === ProjectView.EMPTY) {
      loadProjectView(requireProjectViewPath())
    }
  }

  // Only to be used in monorepo devkit module
  fun forceLoadProjectViewFile(newFile: VirtualFile) {
    logger.info("Forcibly loading project view file content: $newFile")
    loadProjectView(newFile.toNioPath())
  }

  override suspend fun forceReparseCurrentProjectViewFiles(onEachProblem: ((ProjectViewProblem) -> Unit)?) {
    loadProjectView(requireProjectViewPath(), onEachProblem)
    val resolvedImports = projectView
      .imports
      .mapNotNull { it.path.findVirtualFile() }
    edtWriteAction {
      PsiDocumentManager.getInstance(project)
        .reparseFiles(resolvedImports + projectViewFile, false)
    }
  }

  private fun requireProjectViewPath(): Path = projectViewPath ?: error("Missing project view path")

  companion object {
    private val logger = logger<DefaultProjectViewService>()

    @JvmStatic
    fun getInstance(project: Project): DefaultProjectViewService = ProjectViewService.getInstance(project) as DefaultProjectViewService
  }
}

internal val Project.projectViewFile: VirtualFile
  get() = DefaultProjectViewService.getInstance(this).projectViewFile

internal val Project.projectViewFilePsiFile: ProjectViewPsiFile?
  get() = PsiManager.getInstance(this).findFile(projectViewFile) as? ProjectViewPsiFile
