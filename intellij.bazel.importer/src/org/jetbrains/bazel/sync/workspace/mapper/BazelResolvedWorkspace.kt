package org.jetbrains.bazel.sync.workspace

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

@ApiStatus.Internal
data class BazelResolvedWorkspace(
  val workspaceName: String?,
  val repoMapping: RepoMapping,
  val rootTargets: Set<Label>,
  val targets: List<RawBuildTarget>,
  val hasError: Boolean = false,
)
