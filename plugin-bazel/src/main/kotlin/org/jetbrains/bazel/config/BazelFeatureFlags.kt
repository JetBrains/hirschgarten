package org.jetbrains.bazel.config

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bsp.protocol.FeatureFlags

private const val SYMLINK_SCAN_MAX_DEPTH = "bazel.symlink.scan.max.depth"
private const val SHUTDOWN_BEFORE_SHARD_BUILD = "bazel.shutdown.before.shard.build"

object BazelFeatureFlags {
  val symlinkScanMaxDepth: Int
    get() = Registry.intValue(SYMLINK_SCAN_MAX_DEPTH)

  val shutDownBeforeShardBuild: Boolean
    get() = Registry.`is`(SHUTDOWN_BEFORE_SHARD_BUILD)

  // TODO: add starlark debug feature flags
}

class BazelBspFeatureFlagsProvider : FeatureFlagsProvider {
  override fun getFeatureFlags(): FeatureFlags =
    with(BazelFeatureFlags) {
      FeatureFlags(
        bazelSymlinksScanMaxDepth = symlinkScanMaxDepth,
        bazelShutDownBeforeShardBuild = shutDownBeforeShardBuild,
      )
    }
}
