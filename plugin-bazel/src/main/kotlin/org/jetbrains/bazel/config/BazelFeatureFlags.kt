package org.jetbrains.bazel.config

import com.intellij.openapi.util.registry.Registry

private const val SYMLINK_SCAN_MAX_DEPTH = "bazel.symlink.scan.max.depth"

object BazelFeatureFlags {
  val symlinkScanMaxDepth: Int
    get() = Registry.intValue(SYMLINK_SCAN_MAX_DEPTH)

  // TODO: add starlark debug feature flags
}
