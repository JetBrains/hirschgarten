package org.jetbrains.bazel.sync.libraries

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.vfs.AsyncVfsEventsListener
import com.intellij.vfs.AsyncVfsEventsPostProcessor
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.status.SyncStatusListener
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
          val files = provider.getLibraryFiles(project)
          if (files.isNotEmpty()) {
            provider.javaClass to BazelExternalSyntheticLibrary(provider.libraryName, files)
          } else {
            null
          }
        }.toMap()
  }

  private fun initializeListeners() {
    val listener =
      object : AsyncVfsEventsListener {
        override suspend fun filesChanged(events: List<VFileEvent>) {
          if (duringSync.get() || libraries.isEmpty()) return
          val deletedFiles =
            events
              .asSequence()
              .filterIsInstance<VFileDeleteEvent>()
              .map { it.file }
              .toSet()
          if (deletedFiles.isNotEmpty()) {
            libraries.values.forEach { it.removeInvalidFiles(deletedFiles) }
          }
        }
      }

    AsyncVfsEventsPostProcessor.getInstance().addListener(listener, cs)
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
