package org.jetbrains.bsp.protocol

data class ScalaMainClass(
  val className: String,
  val arguments: List<String>,
  val jvmOptions: List<String>,
  val environmentVariables: List<String>? = null,
)
