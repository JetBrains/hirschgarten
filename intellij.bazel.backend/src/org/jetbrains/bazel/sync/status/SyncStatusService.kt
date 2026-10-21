package org.jetbrains.bazel.sync.status

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.ProjectSyncOutcome

internal class SyncAlreadyInProgressException : IllegalStateException()

/**
 * The state of the syncs of one project.
 *
 * @property startedSyncCount The number of syncs that started. A sync that started after a given moment has a higher count.
 * @property lastOutcome How the last sync that finished ended, or null when no sync finished yet. A project that reopens from its cache
 * has no sync, so it has no outcome.
 */
@ApiStatus.Internal
data class SyncStatus(
  val startedSyncCount: Long,
  val isInProgress: Boolean,
  val lastOutcome: ProjectSyncOutcome?,
)

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class SyncStatusService(private val project: Project) {
  @Volatile
  private var isCanceled = false

  private val syncStatusLock = Any()

  private val _status = MutableStateFlow(SyncStatus(startedSyncCount = 0, isInProgress = false, lastOutcome = null))

  /** The state of the syncs. A reader that waits for a change collects this flow. It does not need the lock. */
  val status: StateFlow<SyncStatus> get() = _status

  val isSyncInProgress: Boolean get() = _status.value.isInProgress

  fun <T> withSyncStatus(action: (syncInProgress: Boolean) -> T): T =
    synchronized(syncStatusLock) {
      action(isSyncInProgress)
    }

  fun startSync() {
    synchronized(syncStatusLock) {
      val current = _status.value
      if (current.isInProgress) {
        throw SyncAlreadyInProgressException()
      }
      _status.value = current.copy(startedSyncCount = current.startedSyncCount + 1, isInProgress = true)
      isCanceled = false
    }
    project.messageBus.syncPublisher(SyncStatusListener.TOPIC).syncStarted()
  }

  fun finishSync(outcome: ProjectSyncOutcome) {
    // Read the flag in the same lock as the state change. A waiter can start the next sync as soon as the state changes.
    val canceled = synchronized(syncStatusLock) {
      _status.value = _status.value.copy(isInProgress = false, lastOutcome = outcome)
      isCanceled
    }
    project.messageBus.syncPublisher(SyncStatusListener.TOPIC).syncFinished(canceled)
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

/** Waits until no sync runs, and returns the status at that moment. Returns at once when no sync runs now. */
@ApiStatus.Internal
suspend fun Project.awaitSyncFinished(): SyncStatus = SyncStatusService.getInstance(this).status.first { !it.isInProgress }
