package org.jetbrains.bsp.protocol

data class CppOptionsItem(
  val target: BuildTargetIdentifier,
  val copts: List<String>,
  val defines: List<String>,
  val linkopts: List<String>,
  val linkshared: Boolean? = null,
)
