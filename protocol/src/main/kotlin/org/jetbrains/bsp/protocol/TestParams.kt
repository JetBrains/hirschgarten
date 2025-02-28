package org.jetbrains.bsp.protocol

data class TestParams(
  val targets: List<BuildTargetIdentifier>,
  val originId: String,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val workingDirectory: String? = null,
  val debug: RemoteDebugData? = null,
  val coverage: Boolean? = null,
  val testFilter: String? = null,
  val additionalBazelParams: String? = null,
)
