package org.jetbrains.bazel.sync.task

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.TaskId

internal enum class SyncPhase {
  FIRST,
  SECOND,
}

internal data class SyncWorkspaceContext(
  val phase: SyncPhase,
  val buildProject: Boolean,
  val allKnownTargets: List<Label>?,
  val server: BazelServerFacade,
  val taskId: TaskId,
)

internal enum class SyncWorkspaceStatus {
  SUCCESS,

  // partial error
  PARTIAL,

  // error and no targets
  FATAL,
}
