package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
interface ProjectSyncService {
  val lastSyncTaskId: TaskId?

  suspend fun sync(scope: ProjectSyncScope)
}
