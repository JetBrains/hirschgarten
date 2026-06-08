package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
sealed interface WorkspaceBuildTargetSelector {
  object AllTargets : WorkspaceBuildTargetSelector

  data class SpecificTargets(val targets: List<Label>) : WorkspaceBuildTargetSelector
}

@ApiStatus.Internal
data class WorkspaceBuildTargetParams(
  val selector: WorkspaceBuildTargetSelector,
  val build: Boolean,
  val allTargets: List<Label>?, /* all known targets, if any, from first phase */
  val taskId: TaskId)
