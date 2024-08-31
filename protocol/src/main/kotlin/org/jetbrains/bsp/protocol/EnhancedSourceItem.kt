package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind

class EnhancedSourceItem(
  uri: String,
  kind: SourceItemKind,
  generated: Boolean,
  val data: EnhancedSourceItemData? = null,
) : SourceItem(uri, kind, generated)

data class EnhancedSourceItemData(val jvmPackagePrefix: String)
