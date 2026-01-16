package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

data class BazelResolvedWorkspace(
  val targets: List<RawBuildTarget>,
  val libraries: List<LibraryItem> = listOf(),
  val hasError: Boolean = false,
)
