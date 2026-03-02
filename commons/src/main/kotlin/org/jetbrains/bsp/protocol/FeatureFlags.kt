package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

/**
 * This is NOT the source of truth for feature flag settings.
 * They are set based on registry values in org.jetbrains.bazel.config.BazelFeatureFlags
 */
@ApiStatus.Internal
data class FeatureFlags(
  val isPythonSupportEnabled: Boolean = false,
  val isGoSupportEnabled: Boolean = false,
  val isSharedSourceSupportEnabled: Boolean = false,
  /** Bazel specific */
  val bazelSymlinksScanMaxDepth: Int = 2,
  val bazelShutDownBeforeShardBuild: Boolean = false,
  val isBazelQueryTabEnabled: Boolean = false,
)
