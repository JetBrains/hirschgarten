package org.jetbrains.bazel.sync.status

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean

class SyncAlreadyInProgressException : IllegalStateException()

class SyncFatalFailureException : IllegalStateException()

class SyncPartialFailureException : IllegalStateException()

@Service(Service.Level.PROJECT)
class SyncStatusService(private val project: Project) {
  @Volatile
  private var isCanceled = false

  private val _isSyncInProgress = AtomicBoolean(false)

  val isSyncInProgress: Boolean
    get() = _isSyncInProgress.get()

  fun startSync() {
    if (!_isSyncInProgress.compareAndSet(false, true)) throw SyncAlreadyInProgressException()
    isCanceled = false
    project.messageBus.syncPublisher(SyncStatusListener.TOPIC).syncStarted()
  }

  fun finishSync() {
    _isSyncInProgress.set(false)
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

fun Project.isSyncInProgress() = SyncStatusService.getInstance(this).isSyncInProgress
