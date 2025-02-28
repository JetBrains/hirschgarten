package org.jetbrains.bsp.protocol

data class PackageFeatures(
  val packageId: String,
  val targets: List<BuildTargetIdentifier>,
  val availableFeatures: Map<String, Set<String>>,
  val enabledFeatures: Set<String>,
)
