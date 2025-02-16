package org.jetbrains.plugins.bsp.sync.status

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

class SyncAlreadyInProgressException : IllegalStateException()

@Service(Service.Level.PROJECT)
class BspSyncStatusService(private val project: Project) {
  private var isCanceled = false

  private var _isSyncInProgress: Boolean = false

  val isSyncInProgress: Boolean
    @Synchronized get() = _isSyncInProgress

  @Synchronized
  fun startSync() {
    if (_isSyncInProgress) throw SyncAlreadyInProgressException()
    isCanceled = false
    _isSyncInProgress = true
    project.messageBus.syncPublisher(SyncStatusListener.TOPIC).syncStarted()
  }

  @Synchronized
  fun finishSync() {
    _isSyncInProgress = false
    project.messageBus.syncPublisher(SyncStatusListener.TOPIC).syncFinished(isCanceled)
  }

  @Synchronized
  fun cancel() {
    isCanceled = true
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BspSyncStatusService = project.getService(BspSyncStatusService::class.java)
  }
}

fun Project.isSyncInProgress() = BspSyncStatusService.getInstance(this).isSyncInProgress
