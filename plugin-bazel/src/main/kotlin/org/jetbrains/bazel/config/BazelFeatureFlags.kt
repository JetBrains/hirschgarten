package org.jetbrains.bazel.config

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.plugins.bsp.config.BspFeatureFlagsProvider

private const val SYMLINK_SCAN_MAX_DEPTH = "bazel.symlink.scan.max.depth"
private const val HOTSWAP_ENABLED = "bazel.hotswap.enabled"

object BazelFeatureFlags {
  val symlinkScanMaxDepth: Int
    get() = Registry.intValue(SYMLINK_SCAN_MAX_DEPTH)

  val hotswapEnabled: Boolean
    get() = Registry.`is`(HOTSWAP_ENABLED)

  // TODO: add starlark debug feature flags
}

class BazelBspFeatureFlagsProvider : BspFeatureFlagsProvider {
  override fun getFeatureFlags(): FeatureFlags =
    with(BazelFeatureFlags) {
      FeatureFlags(
        bazelSymlinksScanMaxDepth = symlinkScanMaxDepth,
      )
    }
}
