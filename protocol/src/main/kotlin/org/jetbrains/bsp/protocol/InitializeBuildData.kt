package org.jetbrains.bsp.protocol

data class InitializeBuildData(
  val clientClassesRootDir: String? = null,
  val openTelemetryEndpoint: String? = null,
  val featureFlags: FeatureFlags? = null,
)

data class FeatureFlags(
  val isPythonSupportEnabled: Boolean = false,
  val isAndroidSupportEnabled: Boolean = false,
  val isGoSupportEnabled: Boolean = false,
  val isRustSupportEnabled: Boolean = false,
  val isPropagateExportsFromDepsEnabled: Boolean = true,
)
