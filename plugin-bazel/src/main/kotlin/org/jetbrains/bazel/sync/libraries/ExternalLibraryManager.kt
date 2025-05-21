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

@Service(Service.Level.PROJECT)
class ExternalLibraryManager(private val project: Project, private val cs: CoroutineScope) {
  @Volatile
  private var duringSync: Boolean = false
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
          if (duringSync || libraries.isEmpty()) return
          val deletedFiles =
            events
              .stream()
              .filter { it is VFileDeleteEvent }
              .map { it.file }
              .toList()
              .filterNotNull()
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
          duringSync = true
        }

        override fun targetUtilAvailable() {
          initializeVariables()
          duringSync = false
        }

        override fun syncFinished(canceled: Boolean) {
          initializeVariables()
          duringSync = false
        }
      },
    )
  }

  fun getLibrary(providerClass: Class<out BazelExternalLibraryProvider>): BazelExternalSyntheticLibrary? =
    if (duringSync) null else libraries[providerClass]

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ExternalLibraryManager = project.getService(ExternalLibraryManager::class.java)
  }
}
