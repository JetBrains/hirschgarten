package org.jetbrains.bazel.sync.workspace

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

@ApiStatus.Internal
data class BazelResolvedWorkspace(
  val targets: List<RawBuildTarget>,
  val libraries: List<LibraryItem> = listOf(),
  val fileToTarget: Map<Path, List<Label>> = mapOf(),
  val hasError: Boolean = false,
)
