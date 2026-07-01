package org.jetbrains.bazel.project

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.project.stateStore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import java.nio.file.Path

@ApiStatus.Internal
class DefaultProjectViewService(private val project: Project, coroutineScope: CoroutineScope) : ProjectViewService {

  private val projectViewFile: Deferred<VirtualFile> =
    coroutineScope.async(Dispatchers.IO) {
      findProjectViewVirtualFile()
    }

  private val _projectViewState = MutableStateFlow(ProjectView.EMPTY)

  override val projectViewState: StateFlow<ProjectView>
    get() = _projectViewState

  private suspend fun computeProjectView(): ProjectView {
    val projectViewVirtualFile = projectViewFile.await()
    return parseProjectViewAsync(projectViewVirtualFile) ?: getDefaultProjectView()
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
    if (_projectViewState.value === ProjectView.EMPTY) {
      val newProjectView = computeProjectView()
      _projectViewState.emit(newProjectView)
    }
  }

  override suspend fun forceReparseCurrentProjectViewFiles() {
    logger.info("Reparsing currently opened project view file")
    val newProjectView = computeProjectView()
    _projectViewState.emit(newProjectView)
    val projectViewPath = projectViewFile.await()
    val resolvedImports = newProjectView.imports.mapNotNull { it.resolvedFileOrNull() }
    edtWriteAction {
      PsiDocumentManager.getInstance(project)
        .reparseFiles(resolvedImports + projectViewPath, false)
    }
  }

  fun getProjectViewFileBlocking(): VirtualFile =
    runBlockingMaybeCancellable { projectViewFile.await() }

  @RequiresReadLockAbsence(generateAssertion = false)
  private fun findProjectViewVirtualFile(): VirtualFile {
    // The VFS refresh requires that there be no active read locks;
    // otherwise, it will not perform any action and will not provide a warning.
    ThreadingAssertions.assertNoReadAccess()
    return project.projectViewFilePath?.refreshAndFindVirtualFile() ?: error("Could not find project view file in VFS")
  }

  // Only to be used in monorepo devkit module
  suspend fun forceLoadProjectViewFile(newFile: VirtualFile) {
    logger.info("Forcibly loading project view file content: $newFile")
    val newProjectView = parseProjectViewAsync(newFile) ?: return
    _projectViewState.emit(newProjectView)
  }

  companion object {
    private val logger = logger<DefaultProjectViewService>()

    @JvmStatic
    fun getInstance(project: Project): DefaultProjectViewService = ProjectViewService.getInstance(project) as DefaultProjectViewService
  }
}

internal val Project.projectViewFilePath: Path?
  get() = (stateStore.storeDescriptor as? BazelProjectStoreDescriptor)?.projectViewFile

internal val Project.projectViewFile: VirtualFile
  get() = DefaultProjectViewService.getInstance(this).getProjectViewFileBlocking()

internal val Project.projectViewFilePsiFile: ProjectViewPsiFile?
  get() = PsiManager.getInstance(this).findFile(projectViewFile) as? ProjectViewPsiFile
