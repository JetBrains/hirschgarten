package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class SourceItem(
  override val path: Path,
  val generated: Boolean,
  override var jvmPackagePrefix: String? = null,
) : JvmPrefixSourceFile

interface JvmPrefixSourceFile {
  val path: Path
  var jvmPackagePrefix: String?
}
