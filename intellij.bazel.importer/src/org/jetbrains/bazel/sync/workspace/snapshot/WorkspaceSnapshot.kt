package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap
import java.nio.file.Path

/**
 * Immutable workspace snapshot, describing fixed input scope for [BazelWorkspaceImporter]
 *
 * @property targets Lazy loadable collection of [WorkspaceTarget]
 * @property configurations All used bazel configurations
 * @property targetGraph Immutable representation of bazel target graph
 * @property syncConfigs Set of sync configs used by [BazelWorkspaceImporter] in later stage
 * @property repoMapping Bazel repo mapping
 * @property metadata Snapshot metadata
 */
@ApiStatus.Internal
data class WorkspaceSnapshot(
  val targets: WorkspaceTargetMap,
  val configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration>,
  val targetGraph: WorkspaceTargetGraph,
  val fileToTarget: FileToTargetMap,
  val executableTargets: ExecutableTargetsIndex = ExecutableTargetsIndex.EMPTY,
  val syncConfigs: List<WorkspaceSyncConfig>,
  val repoMapping: RepoMapping,
  val metadata: WorkspaceSnapshotMetadata
) {
  companion object {
    val EMPTY: WorkspaceSnapshot = WorkspaceSnapshot(
      targets = WorkspaceTargetMap.EMPTY,
      configurations = mapOf(),
      targetGraph = WorkspaceTargetGraph.EMPTY,
      fileToTarget = FileToTargetMap.EMPTY,
      syncConfigs = emptyList(),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(
        version = 1,
      )
    )
  }
}

@ApiStatus.Internal
data class WorkspaceSnapshotMetadata(
  val version: Int,
)

/**
 * Immutable [RawBuildTarget] representation
 *
 * @property targetKey Target key
 * @property rawBuildTarget Inner target representation
 */
@ApiStatus.Internal
data class WorkspaceTarget(
  val targetKey: WorkspaceTargetKey,
  val rawBuildTarget: RawBuildTarget,
  val loaded: TargetLoadOptions = TargetLoadOptions.DEFAULT,
) {
  val isFull: Boolean
    get() = loaded == TargetLoadOptions.DEFAULT
}

/**
 * Marker interface for workspace importer specific configuration,
 * Any kind of projectview flag, registry key, or user setting have to be passed
 * using class implementing [WorkspaceSyncConfig] to ensure correct workspace state.
 */
// RC: keep it open, so other modules can extend it
@ApiStatus.Internal
interface WorkspaceSyncConfig

/**
 * Common [WorkspaceSyncConfig] shared among all importers
 *
 * @property projectRootDir Project root directory shall be used instead of [Project.rootDir] to ensure importer pure semantics
 * @property projectName Project name shall be used instead of [Project.bazelProjectName]
 */
@ApiStatus.Internal
data class CommonWorkspaceSyncConfig(
  val projectRootDir: Path,
  val projectName: String,
  val importDepth: Int,
) : WorkspaceSyncConfig
