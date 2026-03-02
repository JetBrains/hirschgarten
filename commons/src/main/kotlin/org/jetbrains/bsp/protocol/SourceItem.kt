package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class SourceItem(
  val path: Path,
  val generated: Boolean,
  var jvmPackagePrefix: String? = null,
)
