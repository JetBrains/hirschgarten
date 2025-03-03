package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class JvmEnvironmentItem(
  val target: Label,
  val classpath: List<String>,
  val jvmOptions: List<String>,
  val workingDirectory: String,
  val environmentVariables: Map<String, String>,
  val mainClasses: List<JvmMainClass>? = null,
)
