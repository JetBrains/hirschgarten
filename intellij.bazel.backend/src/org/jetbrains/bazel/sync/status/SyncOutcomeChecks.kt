package org.jetbrains.bazel.sync.status

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.ProjectSyncCompletionResult
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import kotlin.time.Duration

/**
 * Waits for the sync that runs now, or for a sync that starts in [startWindow]. Then waits until no sync runs, and fails
 * when the last sync did not end in a success. The last sync can also be one that ended before this call.
 *
 * A project that reopens from its cache runs no sync, so it has no outcome. The call then passes after [startWindow].
 */
@ApiStatus.Internal
suspend fun Project.awaitSyncAndCheckOutcome(startWindow: Duration) {
  val status = serviceAsync<SyncStatusService>().status
  val startedSyncCount = status.value.startedSyncCount

  // The sync can start before this call. Then it is already in progress, and the wait ends at once.
  // Otherwise the sync can start later, so the wait has a window. A sync that started and finished in the window also counts.
  withTimeoutOrNull(startWindow) {
    status.first { it.isInProgress || it.startedSyncCount > startedSyncCount }
  }

  // A sync that failed before this call leaves its outcome in the status, so this check sees it too.
  awaitSyncFinished().lastOutcome?.checkSuccessful()
}

/**
 * Waits until no sync runs, runs a sync with [scope], and fails when this sync did not end in a success.
 *
 * Another sync can start between the wait and the call. Then the call is skipped, so this function waits again and retries.
 * A skip with no new sync has another cause, for example an untrusted project. Then this function fails.
 */
@ApiStatus.Internal
suspend fun Project.syncAndCheckOutcome(scope: ProjectSyncScope) {
  val statusService = serviceAsync<SyncStatusService>()
  val syncService = serviceAsync<ProjectSyncService>()
  while (true) {
    val startedSyncCount = awaitSyncFinished().startedSyncCount
    val outcome = syncService.sync(scope)
    val anotherSyncStarted = statusService.status.value.startedSyncCount > startedSyncCount
    if (outcome.completionResult != ProjectSyncCompletionResult.SKIPPED || !anotherSyncStarted) {
      outcome.checkSuccessful()
      return
    }
  }
}
