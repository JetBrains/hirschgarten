package org.jetbrains.bsp.protocol

data class RunParams(
  val target: BuildTargetIdentifier,
  val originId: String? = null,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val workingDirectory: String? = null,
)
