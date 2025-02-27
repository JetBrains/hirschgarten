package org.jetbrains.bsp.protocol

data class CompileParams(
  val targets: List<BuildTargetIdentifier>,
  val originId: String,
  val arguments: List<String>? = null,
)
