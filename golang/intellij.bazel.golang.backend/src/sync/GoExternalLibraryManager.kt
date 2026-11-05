package org.jetbrains.bazel.golang.sync

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsListener
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.entities
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.utils.findVirtualFile
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity
import java.nio.file.Path
import kotlin.io.path.extension

@Service(Service.Level.PROJECT)
internal class GoExternalLibraryManager(private val project: Project) {
  @Volatile
  var library: GoExternalSyntheticLibrary
    private set

  init {
    library = createExternalLibrary()
  }

  fun update() {
    val newLibrary = createExternalLibrary()
    synchronized(this) {
      if (library == newLibrary) return
      library = newLibrary
    }
    fireLibraryChanged(newLibrary)
  }

  private fun createExternalLibrary(): GoExternalSyntheticLibrary {
    val files = getLibraryFiles(project).mapNotNull { it.findVirtualFile() }
    return GoExternalSyntheticLibrary(files)
  }

  private fun getLibraryFiles(project: Project): List<Path> {
    if (!project.isBazelProject) return emptyList()
    val workspacePath = project.rootDir.toNioPathOrNull() ?: return emptyList()

    val libraryFiles =
      project.workspaceModel
        .currentSnapshot
        .entities<BazelGoPackageEntity>()
        .flatMap { it.sources }
        .map { it.toPath() }
        // Files inside the workspace are handled by GoWorkspaceImporter
        .filter { !it.startsWith(workspacePath) && it.extension == "go" }
        .distinct()
        .sorted()  // Make sure resyncs without changes produce the same result
        .toList()
    return libraryFiles
  }

  private fun fireLibraryChanged(library: GoExternalSyntheticLibrary) {
    runWriteAction {
      AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(
        project,
        library.presentableText,
        emptyList(),
        library.allRoots,
        GoExternalLibraryManager::class.java.name,
      )
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): GoExternalLibraryManager = project.service<GoExternalLibraryManager>()
  }
}
