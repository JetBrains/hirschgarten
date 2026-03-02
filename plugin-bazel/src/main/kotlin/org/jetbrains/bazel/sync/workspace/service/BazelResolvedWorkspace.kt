package org.jetbrains.bazel.sync.workspace

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

@ApiStatus.Internal
data class BazelResolvedWorkspace(
  val targets: List<RawBuildTarget>,
  val libraries: List<LibraryItem> = emptyList(),
  val hasError: Boolean = false,
)
