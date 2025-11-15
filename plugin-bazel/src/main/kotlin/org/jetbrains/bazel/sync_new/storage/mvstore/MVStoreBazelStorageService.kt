package org.jetbrains.bazel.sync_new.storage.mvstore

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.sync_new.storage.BazelStorageService
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service(Service.Level.PROJECT)
class MVStoreBazelStorageService(
  private val project: Project
) : BazelStorageService, Disposable, SettingsSavingComponent {
  companion object {
    private val SAVE_DELAY = 5.minutes
  }

  private val disposable = Disposer.newDisposable()
  private var lastSaved = now()

  override val context: MVStoreStorageContext = MVStoreStorageContext(
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
      context.onSave()
      lastSaved = now()
    }
  }

  private fun now() = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)
}
