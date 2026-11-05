package org.jetbrains.bazel.sync.task

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
enum class SyncPhase {
  FIRST,
  SECOND,
}

@ApiStatus.Internal
data class SyncWorkspaceContext(
  val phase: SyncPhase,
  val buildProject: Boolean,
  val allKnownTargets: List<Label>?,
  val knownRepoMapping: RepoMapping?,
  val server: BazelServerFacade,
  val taskId: TaskId,
)

@ApiStatus.Internal
enum class SyncWorkspaceStatus {
  SUCCESS,

  // partial error
  PARTIAL,

  // error and no targets
  FATAL,
}
