package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

sealed interface WorkspaceBuildTargetSelector {
  object AllTargets : WorkspaceBuildTargetSelector

  data class SpecificTargets(val targets: List<Label>) : WorkspaceBuildTargetSelector
}

data class WorkspaceBuildTargetParams(val selector: WorkspaceBuildTargetSelector, val taskId: TaskId)
