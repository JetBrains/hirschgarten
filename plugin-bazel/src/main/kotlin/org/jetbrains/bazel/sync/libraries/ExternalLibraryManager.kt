package org.jetbrains.bazel.sync.libraries

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.util.WaitFor
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.utils.refreshAndFindVirtualFile
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class ExternalLibraryManager(private val project: Project, private val cs: CoroutineScope) {
  private val duringSync: AtomicBoolean = AtomicBoolean(false)
  private val underLibraryUpdate: AtomicBoolean = AtomicBoolean(false)

  @Volatile
  private var libraries: Map<Class<out BazelExternalLibraryProvider>, BazelExternalSyntheticLibrary> = mapOf()

  init {
    if (project.isBazelProject) {
      initializeVariables()
      initializeListeners()
    }
  }

  @Synchronized
  private fun initializeVariables() {
    underLibraryUpdate.set(true)
    // this must be done asynchronously to be able to refresh and find virtual file under read lock
    // https://youtrack.jetbrains.com/issue/BAZEL-2265
    BazelCoroutineService.getInstance(project).start {
      libraries =
        AdditionalLibraryRootsProvider.EP_NAME
          .extensionList
          .mapNotNull { it as? BazelExternalLibraryProvider }
          .mapNotNull { provider ->
            val files = provider.getLibraryFiles(project).mapNotNull { it.refreshAndFindVirtualFile() }
            if (files.isNotEmpty()) {
              provider.javaClass to BazelExternalSyntheticLibrary(provider.libraryName, files)
            } else {
              null
            }
          }.toMap()
      underLibraryUpdate.set(false)
    }
  }

  private fun initializeListeners() {
    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
          duringSync.set(true)
        }

        override fun targetUtilAvailable() {
          handleLibraryUpdate()
        }

        private fun handleLibraryUpdate() {
          if (duringSync.get()) {
            initializeVariables()
            duringSync.set(false)
          }
        }

        override fun syncFinished(canceled: Boolean) {
          handleLibraryUpdate()
        }
      },
    )
  }

  private fun isLibrariesAvailable(): Boolean = !duringSync.get() && !underLibraryUpdate.get()

  /**
   * waits for libraries to be available before returning the corresponding library,
   * maximum waiting time is 10 seconds
   * https://youtrack.jetbrains.com/issue/BAZEL-2283
   */
  fun getLibraryBlocking(providerClass: Class<out BazelExternalLibraryProvider>): BazelExternalSyntheticLibrary? {
    object : WaitFor(10000) {
      override fun condition(): Boolean = isLibrariesAvailable()
    }
    return getLibrary(providerClass)
  }

  fun getLibrary(providerClass: Class<out BazelExternalLibraryProvider>): BazelExternalSyntheticLibrary? =
    if (isLibrariesAvailable()) libraries[providerClass] else null

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ExternalLibraryManager = project.getService(ExternalLibraryManager::class.java)
  }
}
