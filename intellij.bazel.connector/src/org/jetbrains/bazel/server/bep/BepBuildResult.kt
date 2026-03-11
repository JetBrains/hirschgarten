package org.jetbrains.bazel.server.bep

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelProcessResult

@ApiStatus.Internal
data class BepBuildResult(val processResult: BazelProcessResult, val bepOutput: BepOutput)
