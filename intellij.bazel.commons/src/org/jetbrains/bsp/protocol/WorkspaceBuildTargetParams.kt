package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
sealed interface WorkspaceBuildTargetSelector {
  object AllTargets : WorkspaceBuildTargetSelector

  data class SpecificTargets(val targets: List<Label>) : WorkspaceBuildTargetSelector
}

@ApiStatus.Internal
data class WorkspaceBuildTargetParams(val selector: WorkspaceBuildTargetSelector, val taskId: TaskId)
