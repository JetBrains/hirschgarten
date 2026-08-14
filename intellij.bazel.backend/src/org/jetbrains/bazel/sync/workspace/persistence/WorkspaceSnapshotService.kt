package org.jetbrains.bazel.sync.workspace.persistence

import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot

@ApiStatus.Internal
fun interface WorkspaceSnapshotUpdater {
  suspend fun update(snapshot: WorkspaceSnapshot): WorkspaceSnapshot
}

@ApiStatus.Internal
interface WorkspaceSnapshotService {
  val snapshot: StateFlow<WorkspaceSnapshot>

  /**
   * Awaits the initial disk load, then returns the current snapshot.
   * [WorkspaceSnapshot.EMPTY] means no snapshot exists yet.
   */
  suspend fun currentSnapshot(): WorkspaceSnapshot

  suspend fun update(updater: WorkspaceSnapshotUpdater): WorkspaceSnapshot
}
