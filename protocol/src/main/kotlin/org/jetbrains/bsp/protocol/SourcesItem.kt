package org.jetbrains.bsp.protocol

data class SourcesItem(
  val target: BuildTargetIdentifier,
  val sources: List<SourceItem>,
  val roots: List<String> = emptyList(),
)
