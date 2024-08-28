package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItemKind

data class EnhancedSourceItem(
  val uri: String,
  val kind: SourceItemKind,
  val generated: Boolean,
  val data: Any? = null,
)

data class EnhancedSourcesItem(
  val target: BuildTargetIdentifier,
  val roots: List<String>,
  val sources: List<EnhancedSourceItem>,
)

data class EnhancedSourcesResult(
  val items: List<EnhancedSourcesItem>
)


data class EnhancedJvmSourceItemData(
  val packagePrefix: String?,
)
