package org.jetbrains.bsp.protocol

data class SourceItem(
  val uri: String,
  val kind: SourceItemKind,
  val generated: Boolean,
  val jvmPackagePrefix: String? = null,
)
