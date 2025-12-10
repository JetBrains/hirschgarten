package org.jetbrains.bazel.sync_new.storage

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.sync_new.storage.rocksdb.RocksdbStorageContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service(Service.Level.PROJECT)
class BazelStorageService(
  private val project: Project,
) : Disposable, SettingsSavingComponent {
  companion object {
    private val SAVE_DELAY = 5.minutes
  }

  private val disposable = Disposer.newDisposable()
  private var lastSaved = now()

  //val context: StorageContext = InMemoryStorageContext(
  //  project = project,
  //  disposable = disposable,
  //)

   //TODO: finish rocksdb
  val context: StorageContext = RocksdbStorageContext(
    project = project,
    disposable = disposable,
  )

  override fun dispose() {
    Disposer.dispose(disposable)
  }

  override suspend fun save() {
    val exitInProgress = ApplicationManager.getApplication().isExitInProgress
    if (!exitInProgress && (now() - lastSaved) < SAVE_DELAY) {
      return
    }

    withContext(Dispatchers.IO) {
      if (context is LifecycleStoreContext) {
        context.save(force = false)
      }
      lastSaved = now()
    }
  }

  private fun now() = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)
}

val Project.storageContext: StorageContext
  get() = service<BazelStorageService>().context
