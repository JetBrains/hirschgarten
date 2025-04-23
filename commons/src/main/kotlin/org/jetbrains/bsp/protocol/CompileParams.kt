package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class CompileParams(
  val targets: List<Label>,
  val originId: String,
  val arguments: List<String>? = null,
)
