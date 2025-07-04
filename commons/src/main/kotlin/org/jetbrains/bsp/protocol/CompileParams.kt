package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel

data class CompileParams(
  val targets: List<CanonicalLabel>,
  val originId: String,
  val arguments: List<String>? = null,
)
