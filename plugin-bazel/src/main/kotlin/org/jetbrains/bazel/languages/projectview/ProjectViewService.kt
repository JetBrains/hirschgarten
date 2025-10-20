package org.jetbrains.bazel.languages.projectview

import com.google.common.hash.HashCode
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils
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
  val defaultProjectViewVirtualFile: VirtualFile by lazy {
    runBlockingCancellable {
      writeAction {
        val vfs = DummyFileSystem.getInstance()
          .createRoot("tmp")
          .createChildData(null, ".bazelproject")

        val content = ProjectViewFileUtils.projectViewTemplate(project.rootDir).format(".")
        vfs.setBinaryContent(content.toByteArray())

        return@writeAction vfs
      }
    }
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
          overwrite = true,
          projectRootDir = project.rootDir,
          projectViewPath = projectViewPath,
        )
      project.setProjectViewPath(projectViewPath)
    }

    return parseProjectView(projectViewPath)
  }

  private val cachedProjectViewComputable = SyncCache.SyncCacheComputable {
    getProjectView()
  }

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
    val defaultProjectViewVirtualFile = defaultProjectViewVirtualFile
    return runBlockingCancellable {
      val virtualFile = readAction {
        VirtualFileManager.getInstance().findFileByNioPath(projectViewPath)
          ?: defaultProjectViewVirtualFile
      }

      writeAction {
        PsiDocumentManager.getInstance(project)
          .reparseFiles(listOf(virtualFile), false)
      }

      readAction {
        val psi = PsiManager.getInstance(project).findFile(virtualFile) as? ProjectViewPsiFile
          ?: error("Could not parse project view file at $projectViewPath")
        return@readAction ProjectView.fromProjectViewPsiFile(psi)
      }
    }
  }

  fun getProjectViewConfigurationHash(): HashCode = ProjectViewHasher.hash(getProjectView())

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}
