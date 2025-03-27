package org.jetbrains.bsp.protocol

data class BuildTargetCapabilities(
  val canCompile: Boolean = false,
  val canTest: Boolean = false,
  val canRun: Boolean = false,
)

val BuildTargetCapabilities.isExecutable get() = canTest || canRun
