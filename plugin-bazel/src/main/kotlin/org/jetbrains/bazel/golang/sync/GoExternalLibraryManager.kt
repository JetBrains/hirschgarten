package org.jetbrains.bazel.golang.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsListener
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.golang.resolve.BazelGoPackage
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.utils.refreshAndFindVirtualFile
import java.nio.file.Path
import kotlin.io.path.extension

@Service(Service.Level.PROJECT)
class GoExternalLibraryManager(private val project: Project) {
  @Volatile
  var library: GoExternalSyntheticLibrary? = null
    private set

  private val updateMutex = Mutex()

  init {
    update()
  }

  fun update() {
    // this must be done asynchronously to be able to refresh and find virtual file under read lock
    // https://youtrack.jetbrains.com/issue/BAZEL-2265
    BazelCoroutineService.getInstance(project).start {
      doUpdate()
    }
  }

  private suspend fun doUpdate() = updateMutex.withLock {
    val files = getLibraryFiles(project).mapNotNull { it.refreshAndFindVirtualFile() }
    val library = GoExternalSyntheticLibrary(files)
    this.library = library
    fireLibraryChanged(library)
  }

  fun getLibraryFiles(project: Project): List<Path> {
    if (!BazelFeatureFlags.isGoSupportEnabled) return emptyList()
    if (!project.isBazelProject) return emptyList()
    val workspacePath = project.rootDir.toNioPath()

    /**
     * We're not using [BazelGoPackage.goTargetToFileMap] here, because it uses [org.jetbrains.bazel.sync.SyncCache],
     * which may contain data from previous sync while we're executing inside [GoExternalLibraryPostSyncHook].
     */
    val libraryFiles =
      BazelGoPackage
        .getUncachedTargetToFileMap(project)
        .values()
        .filter { !it.startsWith(workspacePath) && it.extension == "go" }
        .distinct()
        .toList()
    return libraryFiles
  }

  private suspend fun fireLibraryChanged(library: GoExternalSyntheticLibrary) = writeAction {
    AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(
      project,
      library.presentableText,
      emptyList(),
      library.allRoots,
      GoExternalLibraryManager::class.java.name,
    )
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): GoExternalLibraryManager = project.service<GoExternalLibraryManager>()
  }
}

private class GoExternalLibraryPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    GoExternalLibraryManager.getInstance(environment.project).update()
  }
}
