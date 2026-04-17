package org.jetbrains.bazel.sync.status

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

internal class SyncAlreadyInProgressException : IllegalStateException()

@Service(Service.Level.PROJECT)
internal class SyncStatusService(private val project: Project) {
  @Volatile
  private var isCanceled = false

  var isSyncInProgress: Boolean = false
    private set

  private val syncStatusLock = Any()

  fun <T> withSyncStatus(action: (syncInProgress: Boolean) -> T): T =
    synchronized(syncStatusLock) {
      action(isSyncInProgress)
    }

  fun startSync() {
    synchronized(syncStatusLock) {
      if (isSyncInProgress) {
        throw SyncAlreadyInProgressException()
      } else {
        isSyncInProgress = true
      }
    }
    isCanceled = false
    project.messageBus.syncPublisher(SyncStatusListener.TOPIC).syncStarted()
  }

  fun finishSync() {
    synchronized(syncStatusLock) {
      isSyncInProgress = false
    }
    project.messageBus.syncPublisher(SyncStatusListener.TOPIC).syncFinished(isCanceled)
  }

  fun cancel() {
    isCanceled = true
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): SyncStatusService = project.getService(SyncStatusService::class.java)
  }
}

fun Project.isSyncInProgress(): Boolean = SyncStatusService.getInstance(this).isSyncInProgress
