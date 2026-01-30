package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.SyncCache

/**
 * Service responsible for parsing and caching ProjectView files.
 * Provides efficient access to project view configuration with automatic cache invalidation.
 */
@Service(Service.Level.PROJECT)
class ProjectViewService(private val project: Project) {
  /**
   * Gets the current ProjectView, parsing it if necessary.
   * Uses caching with file modification time checking for efficiency.
   */
  suspend fun getProjectView(): ProjectView {
    var projectViewPath = findProjectViewPath()

    if (projectViewPath == null || !projectViewPath.exists()) {
      val newProjectView = ProjectViewFileUtils.calculateProjectViewFilePath(
        overwrite = true,
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

    return parseProjectViewAsync(projectViewPath) ?: getDefaultProjectView()
  }

  private val cachedProjectViewComputable = SyncCache.SyncCacheComputable {
    runBlockingMaybeCancellable {
      getProjectView()
    }
  }

  /**
   * Get the cached project view which is reset on every project resync.
   */
  fun getCachedProjectView(): ProjectView = SyncCache.getInstance(project).get(cachedProjectViewComputable)

  private fun findProjectViewPath(): VirtualFile? {
    val pathFromSettings = project.bazelProjectSettings.projectViewPath
    if (pathFromSettings != null && pathFromSettings.exists()) {
      return pathFromSettings
    }
    val rootDir = project.rootDir
    return rootDir.findFile(".bazelproject")
      ?: rootDir.findFileByRelativePath(".bazelbsp/.bazelproject")
  }

  private suspend fun parseProjectViewAsync(projectViewPath: VirtualFile): ProjectView? = readAction {
    (PsiManager.getInstance(project).findFile(projectViewPath) as? ProjectViewPsiFile)
      ?.let { ProjectView.fromProjectViewPsiFile(it) }
  }

  private suspend fun getDefaultProjectView(): ProjectView = readAction  {
    val content = ProjectViewFileUtils.projectViewTemplate(project.rootDir)
    val psiFile = PsiFileFactory.getInstance(project)
      .createFileFromText(".bazelproject", ProjectViewLanguage, content) as ProjectViewPsiFile
    ProjectView.fromProjectViewPsiFile(psiFile)
  }

  suspend fun forceReparseCurrentProjectViewFiles() {
    val projectViewPath = findProjectViewPath() ?: return
    val imports = parseProjectViewAsync(projectViewPath)?.imports ?: emptyList()

    writeAction {
      PsiDocumentManager.getInstance(project)
        .reparseFiles(imports + projectViewPath, false)
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}
