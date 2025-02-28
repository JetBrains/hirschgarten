package org.jetbrains.bazel.sync

import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.SourcesItem

data class BaseTargetInfos(val allTargetIds: List<BuildTargetIdentifier>, val infos: List<BaseTargetInfo>)

data class BaseTargetInfo(
  val target: BuildTarget,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
)
