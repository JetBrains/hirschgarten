package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelPathsResolver

data class WorkspaceBazelPathsResult(
  val bazelBin: String,
  val executionRoot: String,
  val bazelPathsResolver: BazelPathsResolver,
)
