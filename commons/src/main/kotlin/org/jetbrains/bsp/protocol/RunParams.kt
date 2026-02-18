package org.jetbrains.bsp.protocol

import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.label.Label

data class RunParams(
  val taskId: TaskId,
  val target: Label,
  val checkVisibility: Boolean,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val additionalBazelParams: String? = null,
  val pidDeferred: CompletableDeferred<Long?>? = null,
)
