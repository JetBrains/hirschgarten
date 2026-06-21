package org.jetbrains.bazel.sync.workspace.snapshot

import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

/**
 * Unique target identifier
 *
 * @property label Bazel target label
 * @property configuration Unique target configuration identifier can be empty
 */
@ApiStatus.Internal
data class WorkspaceTargetKey(
  val label: Label,
  val configuration: WorkspaceConfigurationId = WorkspaceConfigurationId.EMPTY,
  val aspectIds: WorkspaceAspectIds = WorkspaceAspectIds.EMPTY,
)

@ApiStatus.Internal
@JvmInline
value class WorkspaceAspectIds private constructor(val ids: List<String>) {
  companion object {
    val EMPTY: WorkspaceAspectIds = WorkspaceAspectIds(listOf())

    private val interner = Interner.createWeakInterner<WorkspaceAspectIds>()

    fun of(aspectIds: List<String>): WorkspaceAspectIds =
      when {
        aspectIds.isEmpty() || aspectIds.all { it.isBlank() } -> EMPTY
        else -> interner.intern(WorkspaceAspectIds(aspectIds.filterNot { it.isBlank() }))
      }
  }
}

@ApiStatus.Internal
@JvmInline
value class WorkspaceConfigurationId private constructor(
  val configurationChecksum: String? = null
) {
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

// TODO: move `WorkspaceConfiguration` to backend after importer + connector merge

/**
 * Definition of bazel configuration
 *
 * @property id Unique configuration identifier
 * @property summary Configuration summary taken from BEP
 * @property fragments Bazel configuration fragments
 */
@ApiStatus.Internal
data class WorkspaceConfiguration(
  val id: WorkspaceConfigurationId,
  val summary: WorkspaceConfigurationSummary,
  val fragments: List<WorkspaceConfigurationFragment>,
)

/**
 * Bazel configuration summary emitted from BEP message
 *
 * @property mnemonic Platform mnemonic
 * @property platformName Platform name
 * @property cpu Platform cpu
 * @property makeVariables Bazel make variables, `bazel info --show_make_env`
 * @property isTool Whether this configuration is used for building tools
 */
@ApiStatus.Internal
data class WorkspaceConfigurationSummary(
  val mnemonic: String,
  val platformName: String,
  val cpu: String,
  val makeVariables: Map<String, String>,
  val isTool: Boolean,
)

/**
 * Marker interface for bazel configuration fragments
 *
 * **NOTE:** Each rule-specific fragment definition shall live in dedicated module
 */
@ApiStatus.Internal
interface WorkspaceConfigurationFragment
