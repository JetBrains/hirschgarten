package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

class WorkspaceBuildPartialTargetsParams(
  val targets: List<Label>,
  val repoMapping: RepoMapping
)

data class WorkspaceBuildPartialTargetsResult(
  val rootTargets: Set<Label>,
  val targets: Map<Label, BspTargetInfo.TargetInfo>
)
