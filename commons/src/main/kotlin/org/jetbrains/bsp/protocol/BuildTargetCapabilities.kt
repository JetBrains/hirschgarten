package org.jetbrains.bsp.protocol

internal data class BuildTargetCapabilities(
  val canCompile: Boolean = false,
  val canTest: Boolean = false,
  val canRun: Boolean = false,
)

internal val BuildTargetCapabilities.isExecutable get() = canTest || canRun
