package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bsp.protocol.BuildTarget

// contains minimal set of information needed for WorkspaceSnapshot construction/merge,
// it allows to avoid unnecessary building of target graph and indexes
@ApiStatus.Internal
data class IncompleteWorkspaceSnapshot(
  val targets: Map<WorkspaceTargetKey, BuildTarget>,
  val rootTargets: Set<WorkspaceTargetKey>,
  val configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration>,
  val repoMapping: RepoMapping,
  val workspaceName: String?,
)

@ApiStatus.Internal
fun WorkspaceSnapshot.toIncompleteSnapshot(): IncompleteWorkspaceSnapshot =
  IncompleteWorkspaceSnapshot(
    targets = this.targets.allTargets().associateBy { it.key },
    rootTargets = this.targetGraph.rootTargets.toHashSet(),
    configurations = this.configurations,
    repoMapping = this.repoMapping,
    workspaceName = this.workspaceName,
  )
