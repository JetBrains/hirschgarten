package org.jetbrains.bsp.protocol

data class RustPackage(
  val id: String,
  val rootUrl: String,
  val name: String,
  val version: String,
  val origin: String,
  val edition: String,
  val resolvedTargets: List<RustTarget>,
  val allTargets: List<RustTarget>,
  val features: Map<String, Set<String>>,
  val enabledFeatures: Set<String>,
  val source: String? = null,
  val cfgOptions: Map<String, List<String>>? = null,
  val env: Map<String, String>? = null,
  val outDirUrl: String? = null,
  val procMacroArtifact: String? = null,
)
