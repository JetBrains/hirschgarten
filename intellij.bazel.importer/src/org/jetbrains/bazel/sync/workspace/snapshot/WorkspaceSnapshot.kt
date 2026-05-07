package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import java.nio.file.Path

/**
 * Immutable workspace snapshot, describing fixed input scope for [BazelWorkspaceImporter]
 *
 * @property targets Full set of [WorkspaceTarget]
 * @property configurations All used bazel configurations
 * @property targetGraph Immutable representation of bazel target graph
 * @property syncConfigs Set of sync configs used by [BazelWorkspaceImporter] in later stage
 * @property repoMapping Bazel repo mapping
 * @property hasError Did previous steps ended with partial failure
 */
@ApiStatus.Internal
data class WorkspaceSnapshot(
  val targets: Map<WorkspaceTargetKey, WorkspaceTarget>,
  val configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration>,
  val targetGraph: WorkspaceTargetGraph,

  // MAYBE RC: this feels wrong, for persistent version I'm going to make this prettier
  val fileToTarget: Map<Path, List<WorkspaceTargetKey>> = mapOf(),
  val syncConfigs: List<WorkspaceSyncConfig>,
  val repoMapping: RepoMapping,
  val hasError: Boolean,
)

/**
 * Unique [WorkspaceTarget] identifier
 *
 * @property label Bazel target label
 * @property configuration Unique target configuration identifier can be empty
 */
@ApiStatus.Internal
data class WorkspaceTargetKey(
  val label: Label,
  val configuration: WorkspaceConfigurationId = WorkspaceConfigurationId.EMPTY,
)

@ApiStatus.Internal
@JvmInline
value class WorkspaceConfigurationId private constructor(val configurationHash: String? = null) {
  companion object {
    val EMPTY: WorkspaceConfigurationId = WorkspaceConfigurationId(null)

    // RC: force `WorkspaceConfigurationId` normalization to avoid
    //  unexpected failing equality checks, don't ask how I know...
    fun of(configurationHash: String?): WorkspaceConfigurationId = when {
      configurationHash.isNullOrBlank() -> EMPTY
      else -> WorkspaceConfigurationId(configurationHash)
    }
  }
}

/**
 * Definition of bazel configuration
 *
 * @property id Unique configuration identifier
 */
// TODO: fill with useful properties
@ApiStatus.Internal
data class WorkspaceConfiguration(
  val id: WorkspaceConfigurationId,
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
)

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
) : WorkspaceSyncConfig
