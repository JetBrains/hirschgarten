package org.jetbrains.bazel.sync.workspace

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

@ApiStatus.Internal
data class BazelResolvedWorkspace(
  val rootTargets: Set<Label>,
  val targets: List<RawBuildTarget>,
  val fileToTarget: Map<Path, List<RawBuildTarget>> = mapOf(),
  val hasError: Boolean = false,
)
