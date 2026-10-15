package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
interface ProjectSyncService {
  val lastSyncTaskId: TaskId?

  /**
   * Runs a sync and returns how it ended. The result is [ProjectSyncCompletionResult.SKIPPED] when another sync is running
   * and when the project is not trusted.
   */
  suspend fun sync(scope: ProjectSyncScope): ProjectSyncOutcome
}
