package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.GoLibraryItem
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

data class BazelResolvedWorkspace(
  val targets: Map<Label, RawBuildTarget> = mapOf(),
  val libraries: List<LibraryItem> = listOf(),
  val goLibraries: List<GoLibraryItem> = listOf(),
  val hasError: Boolean = false,
) {

}
