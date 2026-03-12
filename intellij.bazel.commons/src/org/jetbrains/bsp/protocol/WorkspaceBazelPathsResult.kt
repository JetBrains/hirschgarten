package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver

@ApiStatus.Internal
data class WorkspaceBazelPathsResult(
  val bazelBin: String,
  val executionRoot: String,
  val bazelPathsResolver: BazelPathsResolver,
)
