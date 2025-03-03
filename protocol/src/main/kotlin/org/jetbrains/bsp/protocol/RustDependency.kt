package org.jetbrains.bsp.protocol

data class RustDependency(
  val pkg: String,
  val name: String? = null,
  val depKinds: List<RustDepKindInfo>? = null,
)
