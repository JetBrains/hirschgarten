package org.jetbrains.bazel.sync.libraries

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.bazel.config.isBazelProject
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

  private fun initializeVariables() {
    this.libraries =
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
  }

  private fun initializeListeners() {
    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
          duringSync.set(true)
          underLibraryUpdate.set(false)
        }

        override fun targetUtilAvailable() {
          handleLibraryUpdate()
        }

        private fun handleLibraryUpdate() {
          if (duringSync.get() && underLibraryUpdate.compareAndSet(false, true)) {
            initializeVariables()
            duringSync.set(false)
            underLibraryUpdate.set(false)
          }
        }

        override fun syncFinished(canceled: Boolean) {
          handleLibraryUpdate()
        }
      },
    )
  }

  @Synchronized
  fun getLibrary(providerClass: Class<out BazelExternalLibraryProvider>): BazelExternalSyntheticLibrary? =
    if (duringSync.get()) null else libraries[providerClass]

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ExternalLibraryManager = project.getService(ExternalLibraryManager::class.java)
  }
}
