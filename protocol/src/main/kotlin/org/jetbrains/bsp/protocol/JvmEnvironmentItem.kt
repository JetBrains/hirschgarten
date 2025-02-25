package org.jetbrains.bsp.protocol

data class JvmEnvironmentItem(
  val target: BuildTargetIdentifier,
  val classpath: List<String>,
  val jvmOptions: List<String>,
  val workingDirectory: String,
  val environmentVariables: Map<String, String>,
  val mainClasses: List<JvmMainClass>? = null,
)
