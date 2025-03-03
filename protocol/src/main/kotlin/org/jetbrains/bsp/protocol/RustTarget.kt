package org.jetbrains.bsp.protocol

data class RustTarget(
  val name: String,
  val crateRootUrl: String,
  val kind: RustTargetKind,
  val edition: String,
  val doctest: Boolean,
  val crateTypes: List<RustCrateType>? = null,
  val requiredFeatures: Set<String>? = null,
)
