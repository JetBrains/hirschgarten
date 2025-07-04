package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel

data class TestParams(
  val targets: List<CanonicalLabel>,
  val originId: String,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val workingDirectory: String? = null,
  val debug: DebugType? = null,
  val coverage: Boolean? = null,
  val testFilter: String? = null,
  val additionalBazelParams: String? = null,
)
