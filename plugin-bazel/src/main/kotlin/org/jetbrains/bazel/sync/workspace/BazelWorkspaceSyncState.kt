package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.EarlyBazelSyncProject

internal sealed interface BazelWorkspaceSyncState {
  val earlyProject: EarlyBazelSyncProject
  val mappedProject: BazelMappedProject
  val resolvedWorkspace: BazelResolvedWorkspace

  sealed class Nothing(val error: String) : BazelWorkspaceSyncState {
    override val earlyProject: EarlyBazelSyncProject
      get() = error(error)
    override val mappedProject: BazelMappedProject
      get() = error(error)
    override val resolvedWorkspace: BazelResolvedWorkspace
      get() = error(error)
  }

  object NotInitialized : Nothing("project is not initialized")

  // for example when project is being opened
  // TODO: we can avoid this state by serializing project state and restoring it on project open
  object Unsynced : Nothing("project is not synced")

  object Initialized : Nothing("project is not synced")

  // project only has raw target data
  data class Synced(
      override val earlyProject: EarlyBazelSyncProject,
  ) : BazelWorkspaceSyncState {
    override val mappedProject: BazelMappedProject
      get() = error("project is not resolved")
    override val resolvedWorkspace: BazelResolvedWorkspace
      get() = error("project is not resolved")
  }

  // project is fully resolved
  data class Resolved(
      override val earlyProject: EarlyBazelSyncProject,
      override val mappedProject: BazelMappedProject,
      override val resolvedWorkspace: BazelResolvedWorkspace,
  ) : BazelWorkspaceSyncState
}
