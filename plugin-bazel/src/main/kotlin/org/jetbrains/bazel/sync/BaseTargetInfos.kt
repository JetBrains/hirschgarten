package org.jetbrains.bazel.sync

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.SourcesItem

data class BaseTargetInfos(
  val allTargetIds: List<Label>,
  val infos: List<BaseTargetInfo>,
  val hasError: Boolean = false,
)

data class BaseTargetInfo(
  val target: BuildTarget,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
)
