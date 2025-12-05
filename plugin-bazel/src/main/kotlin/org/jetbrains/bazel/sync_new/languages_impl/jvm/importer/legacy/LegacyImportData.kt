package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.legacy

import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

data class LegacyImportData(
  val targets: List<RawBuildTarget>,
  val libraries: List<LibraryItem>
)
