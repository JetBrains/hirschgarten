package org.jetbrains.bazel.sync.workspace.persistence

import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot

@ApiStatus.Internal
fun interface WorkspaceSnapshotUpdater<R> {
  suspend fun update(snapshot: WorkspaceSnapshot): Pair<WorkspaceSnapshot, R>
}

@ApiStatus.Internal
interface WorkspaceSnapshotService {
  val snapshot: StateFlow<WorkspaceSnapshot>

  /**
   * Awaits the initial disk load, then returns the current snapshot.
   * [WorkspaceSnapshot.EMPTY] means no snapshot exists yet.
   */
  suspend fun currentSnapshot(): WorkspaceSnapshot

  /**
   * Update current snapshot atomically
   */
  suspend fun <R> update(updater: WorkspaceSnapshotUpdater<R>): Pair<WorkspaceSnapshot, R>
}
