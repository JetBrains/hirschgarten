package org.jetbrains.bazel.server.bsp.managers

import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.server.bep.BepOutput

data class BepBuildResult(val processResult: BazelProcessResult, val bepOutput: BepOutput)
