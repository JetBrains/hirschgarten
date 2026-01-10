package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class SourceItem(
  val path: Path,
  val generated: Boolean,
  var jvmPackagePrefix: String? = null,
)
