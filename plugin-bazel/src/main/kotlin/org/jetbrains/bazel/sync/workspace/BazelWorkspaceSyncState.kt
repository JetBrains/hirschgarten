package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.EarlyBazelSyncProject

data class SyncedWorkspaceState(
  val earlyProject: EarlyBazelSyncProject
)

data class ResolvedWorkspaceState(
  val mappedProject: BazelMappedProject,
  val resolvedWorkspace: BazelResolvedWorkspace,
)

internal sealed interface BazelWorkspaceSyncState {

  sealed class Nothing(val reason: String) : BazelWorkspaceSyncState

  object NotInitialized : Nothing("project is not initialized")

  // for example when project is being opened
  // TODO: we can avoid this state by serializing project state and restoring it on project open
  object Unsynced : Nothing("project is not synced")

  object Initialized : Nothing("project is not synced")

  // project only has raw target data
  data class Synced(
      val synced: SyncedWorkspaceState
  ) : BazelWorkspaceSyncState

  // project is fully resolved
  data class Resolved(
    val synced: SyncedWorkspaceState,
    val resolved: ResolvedWorkspaceState
  ) : BazelWorkspaceSyncState
}
