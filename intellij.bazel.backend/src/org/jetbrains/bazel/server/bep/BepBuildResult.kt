package org.jetbrains.bazel.server.bep

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.commons.BazelStatus

@ApiStatus.Internal
data class BepBuildResult(
  val processResult: BazelProcessResult,
  val bepOutput: BepOutput,
  val buildStatus: BazelStatus,
)
