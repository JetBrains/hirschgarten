package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class TestParams(
  val taskId: TaskId,
  val targets: List<Label>,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val debug: DebugType? = null,
  val coverageInstrumentationFilter: String? = null,
  val testFilter: String? = null,
  val additionalBazelParams: String? = null,
  val useJetBrainsTestRunner: Boolean = false,
)
