package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class TestParams(
  val taskId: TaskId,
  val targets: List<Label>,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val coverageInstrumentationFilter: String? = null,
  val testFilter: String? = null,
  val additionalBazelParams: String? = null,
  val useJetBrainsTestRunner: Boolean = false,
)
