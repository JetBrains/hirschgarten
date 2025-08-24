package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

@JvmInline
value class RawAspectTarget(val target: BspTargetInfo.TargetInfo)

data class WorkspaceBuildTargetsResult(
  val targets: Map<Label, RawAspectTarget>,
  val rootTargets: Set<Label>,
  val hasError: Boolean = false,
)
