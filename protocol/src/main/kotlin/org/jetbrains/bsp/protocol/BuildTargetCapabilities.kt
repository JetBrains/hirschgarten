package org.jetbrains.bsp.protocol

data class BuildTargetCapabilities(
  val canCompile: Boolean? = null,
  val canTest: Boolean? = null,
  val canRun: Boolean? = null,
  val canDebug: Boolean? = null,
)
