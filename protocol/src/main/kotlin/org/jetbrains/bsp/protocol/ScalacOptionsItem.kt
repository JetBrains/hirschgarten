package org.jetbrains.bsp.protocol

data class ScalacOptionsItem(
  val target: BuildTargetIdentifier,
  val options: List<String>,
  val classpath: List<String>,
  val classDirectory: String,
)
