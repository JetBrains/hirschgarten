package org.jetbrains.bazel.languages.projectview

import com.google.common.hash.HashCode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.settings.bazel.setProjectViewPath
import org.jetbrains.bazel.sync.SyncCache
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists

/**
 * Service responsible for parsing and caching ProjectView files.
 * Provides efficient access to project view configuration with automatic cache invalidation.
 */
@Service(Service.Level.PROJECT)
class ProjectViewService(private val project: Project) {
  private val log = logger<ProjectViewService>()
  fun getDefaultProjectViewFileContent(): ProjectView {
    val content = ProjectViewFileUtils.projectViewTemplate(project).format(".")
    val psiFile = PsiFileFactory.getInstance(project)
      .createFileFromText(ProjectViewLanguage, content) as ProjectViewPsiFile
    return ProjectView.fromProjectViewPsiFile(psiFile)
  }

  /**
   * Gets the current ProjectView, parsing it if necessary.
   * Uses caching with file modification time checking for efficiency.
   */
  fun getProjectView(): ProjectView {
    var projectViewPath = findProjectViewPath()

    if (projectViewPath.notExists()) {
      projectViewPath =
        ProjectViewFileUtils.calculateProjectViewFilePath(
          project = project,
          generateContent = true,
          overwrite = true,
          bazelPackageDir = null,
        )
      project.setProjectViewPath(projectViewPath)
    }

    return parseProjectView(projectViewPath)
  }

  private val cachedProjectViewComputable = SyncCache.SyncCacheComputable {
    getProjectView()
  }

  /**
   * Get the cached project view which is reset on every project resync.
   */
  fun getCachedProjectView(): ProjectView = SyncCache.getInstance(project).get(cachedProjectViewComputable)

  private fun findProjectViewPath(): Path {
    val pathFromSettings = project.bazelProjectSettings.projectViewPath
    if (pathFromSettings != null && pathFromSettings.exists()) {
      return pathFromSettings
    }
    val rootDir = project.rootDir.toNioPath()
    val projectViewPath = rootDir.resolve(".bazelproject")

    return if (projectViewPath.exists()) {
      projectViewPath
    } else {
      rootDir.resolve(".bazelbsp/.bazelproject")
    }
  }

  private fun parseProjectView(projectViewPath: Path): ProjectView {
    return runBlockingCancellable {
      val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(projectViewPath)
        ?: return@runBlockingCancellable readAction { getDefaultProjectViewFileContent() }

      val psiFile = readAction { PsiManager.getInstance(project).findFile(virtualFile) }

      ensureCorrectFileType(virtualFile, psiFile, projectViewPath)

      val projectView = readAction {
        val psi = psiFile as? ProjectViewPsiFile ?: return@readAction null
        ProjectView.fromProjectViewPsiFile(psi)
      }

      return@runBlockingCancellable projectView
        ?: readAction { getDefaultProjectViewFileContent() }
    }
  }

  /**
   * Ensures that .bazelproject file has correct ProjectView file type.
   * Attempts auto-fix if needed, throws exception if fails.
   */
  private fun ensureCorrectFileType(virtualFile: VirtualFile, psiFile: PsiFile?, projectViewPath: Path) {
    if (psiFile is ProjectViewPsiFile) {
      return
    }

    if (psiFile != null) {
      log.warn("ProjectView file type not recognized: path=$projectViewPath, fileType=${virtualFile.fileType.name}, language=${psiFile.language}")

      if (!tryAutoFixFileTypeRegistration(virtualFile)) {
        throw IllegalStateException("""
          .bazelproject file not recognized as ProjectView file type.

          Please fix manually:
          1. Go to Settings → Editor → File Types
          2. Find "ProjectView file for Bazel project" in the list
          3. Add "*.bazelproject" to File name patterns
          4. Restart IDE and try sync again
        """.trimIndent())
      }
    }
  }

  /**
   * Attempts to auto-fix ProjectView file type registration by associating *.bazelproject pattern
   * with ProjectViewFileType.
   * 
   * @param virtualFile the .bazelproject file that needs to be fixed
   * @return true if auto-fix succeeded, false if it failed
   */
  private fun tryAutoFixFileTypeRegistration(virtualFile: VirtualFile): Boolean {
    log.info("Attempting to auto-fix ProjectView file type registration...")

    return try {
      ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runWriteAction {
          val fileTypeManager = FileTypeManager.getInstance()
          fileTypeManager.associatePattern(ProjectViewFileType, "*.bazelproject")
        }
      }

      // Force refresh the virtual file to pick up new file type
      virtualFile.refresh(false, false)

      log.info("Auto-fix completed: file type registration updated")
      true
    } catch (e: ProcessCanceledException) {
      throw e
    } catch (e: Exception) {
      log.warn("Auto-fix failed with exception: ${e.message}", e)
      false
    }
  }

  fun getProjectViewConfigurationHash(): HashCode = ProjectViewHasher.hash(getProjectView())

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}
