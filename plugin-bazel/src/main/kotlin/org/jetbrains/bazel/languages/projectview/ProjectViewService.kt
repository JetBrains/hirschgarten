package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

/**
 * Service responsible for parsing and caching ProjectView files.
 * Provides efficient access to project view configuration with automatic cache invalidation.
 */
@Service(Service.Level.PROJECT)
class ProjectViewService(private val project: Project) {
  private var cachedProjectView: ProjectView? = null
  private var cachedProjectViewPath: Path? = null
  private var cachedProjectViewModTime: FileTime? = null

  /**
   * Gets the current ProjectView, parsing it if necessary.
   * Uses caching with file modification time checking for efficiency.
   */
  fun getProjectView(): ProjectView {
    val projectViewPath = findProjectViewPath()
    val currentModTime = getFileModificationTime(projectViewPath)

    // Return cached version if path and modification time haven't changed
    if (isCacheValid(projectViewPath, currentModTime)) {
      return cachedProjectView!!
    }

    // Parse the project view
    val projectView = parseProjectView(projectViewPath)

    // Update cache
    updateCache(projectView, projectViewPath, currentModTime)

    return projectView
  }

  private fun getFileModificationTime(path: Path): FileTime? =
    try {
      if (path.exists()) path.getLastModifiedTime() else null
    } catch (e: Exception) {
      null // If we can't get modification time, force reparse
    }

  private fun isCacheValid(projectViewPath: Path, currentModTime: FileTime?): Boolean =
    cachedProjectView != null &&
      cachedProjectViewPath == projectViewPath &&
      cachedProjectViewModTime == currentModTime

  private fun updateCache(
    projectView: ProjectView,
    projectViewPath: Path,
    modTime: FileTime?,
  ) {
    cachedProjectView = projectView
    cachedProjectViewPath = projectViewPath
    cachedProjectViewModTime = modTime
  }

  private fun findProjectViewPath(): Path {
    val rootDir = project.rootDir.toNioPath()
    val projectViewPath = rootDir.resolve(".bazelproject")

    if (projectViewPath.notExists()) {
      generateEmptyProjectView(projectViewPath)
    }

    return projectViewPath
  }

  private fun parseProjectView(projectViewPath: Path): ProjectView =
    ReadAction.compute<ProjectView, RuntimeException> {
      val virtualFile =
        VirtualFileManager.getInstance().findFileByNioPath(projectViewPath)
          ?: error("Could not find project view file at $projectViewPath")

      val psiFile =
        PsiManager.getInstance(project).findFile(virtualFile) as? ProjectViewPsiFile
          ?: error("Could not parse project view file at $projectViewPath")

      ProjectView.fromProjectViewPsiFile(psiFile)
    }

  private fun generateEmptyProjectView(projectViewPath: Path) {
    // Create an empty .bazelproject file
    projectViewPath.toFile().writeText("")
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}
