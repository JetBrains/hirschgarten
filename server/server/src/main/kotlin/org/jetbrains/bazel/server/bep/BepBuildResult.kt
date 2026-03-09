package org.jetbrains.bazel.server.bep

import org.jetbrains.bazel.bazelrunner.BazelProcessResult

internal data class BepBuildResult(val processResult: BazelProcessResult, val bepOutput: BepOutput)
