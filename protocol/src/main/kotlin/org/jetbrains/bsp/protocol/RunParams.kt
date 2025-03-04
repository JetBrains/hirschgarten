package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class RunParams(
  val target: Label,
  val originId: String? = null,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val workingDirectory: String? = null,
)
