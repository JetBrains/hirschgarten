package org.jetbrains.bsp.protocol

import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.label.CanonicalLabel

data class RunParams(
  val target: CanonicalLabel,
  val originId: String,
  val arguments: List<String>? = null,
  val environmentVariables: Map<String, String>? = null,
  val workingDirectory: String? = null,
  val additionalBazelParams: String? = null,
  val pidDeferred: CompletableDeferred<Long?>? = null,
)
