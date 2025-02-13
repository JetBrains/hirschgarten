package org.jetbrains.plugins.bsp.sync

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem

data class BaseTargetInfos(val allTargetIds: List<BuildTargetIdentifier>, val infos: List<BaseTargetInfo>)

data class BaseTargetInfo(
  val target: BuildTarget,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
)
