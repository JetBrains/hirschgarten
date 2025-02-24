package org.jetbrains.bsp.protocol

data class RustRawDependency(
  val name: String,
  val rename: String? = null,
  val kind: String? = null,
  val target: String? = null,
  val optional: Boolean,
  val usesDefaultFeatures: Boolean,
  val features: Set<String>,
)
