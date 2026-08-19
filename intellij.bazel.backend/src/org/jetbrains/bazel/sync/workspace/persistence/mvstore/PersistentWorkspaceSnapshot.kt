package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path

// contain lightweight parts of `WorkspaceSnapshot` which
// shall be serialized into on disk blob
@ApiStatus.Internal
class PersistentWorkspaceSnapshot(

  // direct references inside `WorkspaceSnapshot`
  val workspaceName: String?,
  val configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration>,
  val targetGraph: WorkspaceTargetGraph,
  val syncConfigs: List<WorkspaceSyncConfig>,
  val repoMapping: RepoMapping,
  val metadata: WorkspaceSnapshotMetadata,

  // extra things
  val keyId2Target: Int2ObjectBiMap<WorkspaceTargetKey>,
  val labelId2Label: Int2ObjectBiMap<Label>
)

@ApiStatus.Internal
class PartialWorkspaceTarget(
  val kind: TargetKind,
  val baseDirectory: Path,
  val generatorName: String?,
  val isWorkspace: Boolean,
  val isTestOnly: Boolean,
  val tags: List<String>
)

@ApiStatus.Internal
class WorkspaceTargetDeps(
  val dependencies: List<DependencyLabel>,
)

@ApiStatus.Internal
class HeavyWorkspaceTarget(
  val sources: SourceFileCollection,
  val generatedSources: SourceFileCollection,
  val resources: SourceFileCollection,
)

@ApiStatus.Internal
class WorkspaceTargetToSave(
  val keyId: Int,
  val target: BuildTarget,
)
