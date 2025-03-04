package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class PackageFeatures(
  val packageId: String,
  val targets: List<Label>,
  val availableFeatures: Map<String, Set<String>>,
  val enabledFeatures: Set<String>,
)
