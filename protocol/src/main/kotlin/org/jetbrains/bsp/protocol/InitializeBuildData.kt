package org.jetbrains.bsp.protocol

import kotlin.math.max

data class FeatureFlags(
  val isPythonSupportEnabled: Boolean = false,
  val isAndroidSupportEnabled: Boolean = false,
  val isGoSupportEnabled: Boolean = false,
  val isRustSupportEnabled: Boolean = false,
  val isCppSupportEnabled: Boolean = false,
  val isPropagateExportsFromDepsEnabled: Boolean = true,
  /** Bazel specific */
  val bazelSymlinksScanMaxDepth: Int = 2,
  val bazelShutDownBeforeShardBuild: Boolean = false,
) {
  fun merge(anotherFeatureFlags: FeatureFlags): FeatureFlags =
    FeatureFlags(
      isPythonSupportEnabled = isPythonSupportEnabled || anotherFeatureFlags.isPythonSupportEnabled,
      isAndroidSupportEnabled = isAndroidSupportEnabled || anotherFeatureFlags.isAndroidSupportEnabled,
      isGoSupportEnabled = isGoSupportEnabled || anotherFeatureFlags.isGoSupportEnabled,
      isRustSupportEnabled = isRustSupportEnabled || anotherFeatureFlags.isRustSupportEnabled,
      isPropagateExportsFromDepsEnabled = isPropagateExportsFromDepsEnabled || anotherFeatureFlags.isPropagateExportsFromDepsEnabled,
      bazelSymlinksScanMaxDepth = max(bazelSymlinksScanMaxDepth, anotherFeatureFlags.bazelSymlinksScanMaxDepth),
    )
}
