package org.jetbrains.bazel.languages.projectview

import com.google.common.hash.HashCode
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.runBlockingCancellable
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
  fun getProjectView(): ProjectView {
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

    return parseProjectView(projectViewPath)
  }

  private val cachedProjectViewComputable = SyncCache.SyncCacheComputable {
    getProjectView()
  }

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

  private fun parseProjectView(projectViewPath: VirtualFile): ProjectView {
    return runBlockingCancellable {
      parseProjectViewAsync(projectViewPath) ?: getDefaultProjectView()
    }
  }

  private suspend fun parseProjectViewAsync(projectViewPath: VirtualFile): ProjectView? = readAction {
    val psi = PsiManager.getInstance(project).findFile(projectViewPath) as? ProjectViewPsiFile
      ?: return@readAction null
    return@readAction ProjectView.fromProjectViewPsiFile(psi)
  }

  private fun getDefaultProjectView(): ProjectView {
    val content = ProjectViewFileUtils.projectViewTemplate(project.rootDir).format(".")
    val psiFile = PsiFileFactory.getInstance(project)
      .createFileFromText(".bazelproject", ProjectViewLanguage, content) as ProjectViewPsiFile
    return ProjectView.fromProjectViewPsiFile(psiFile)
  }

  suspend fun forceReparseCurrentProjectViewFiles() {
    val projectViewPath = findProjectViewPath() ?: return
    val imports = parseProjectViewAsync(projectViewPath)?.imports ?: emptyList()

    writeAction {
      PsiDocumentManager.getInstance(project)
        .reparseFiles(imports + projectViewPath, false)
    }
  }

  fun getProjectViewConfigurationHash(): HashCode = ProjectViewHasher.hash(getProjectView())

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}
