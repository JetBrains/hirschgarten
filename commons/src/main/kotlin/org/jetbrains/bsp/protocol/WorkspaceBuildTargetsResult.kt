package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

data class WorkspaceBuildTargetsResult(
  val targets: Map<Label, BspTargetInfo.TargetInfo>,
  val rootTargets: Set<Label>,
  val hasError: Boolean = false,
)
