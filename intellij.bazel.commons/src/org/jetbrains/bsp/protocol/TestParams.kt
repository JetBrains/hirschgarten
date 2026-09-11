package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class TestParams(
  val taskId: TaskId,
  val targets: List<Label>,
  val arguments: List<String> = emptyList(),
  val environmentVariables: Map<String, String> = emptyMap(),
  val useCoverage: Boolean = false,
  val coverageInstrumentationFilter: String? = null,
  val testFilter: String? = null,
  val additionalBazelParams: List<String> = emptyList(),
  val streamTestOutput: Boolean = false,
)
