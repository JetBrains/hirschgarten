package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class WorkspaceBuildTargetsResult(
  val targets: Map<Label, BspTargetInfo.TargetInfo>,
  val rootTargets: Set<Label>,
  val hasError: Boolean = false,
)
