package org.jetbrains.bazel.server.bep

import org.jetbrains.bazel.bazelrunner.BazelProcessResult

data class BepBuildResult(val processResult: BazelProcessResult, val bepOutput: BepOutput)
