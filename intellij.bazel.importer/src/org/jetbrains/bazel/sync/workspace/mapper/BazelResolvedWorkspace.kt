package org.jetbrains.bazel.sync.workspace

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
data class BazelResolvedWorkspace(
  val workspaceName: String?,
  val repoMapping: RepoMapping,
  val rootTargets: Set<WorkspaceTargetKey>,
  val targets: List<BuildTarget>,
  val hasError: Boolean = false,
  val configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration>
)
