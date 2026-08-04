package org.jetbrains.bazel.clion.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.extractData

@ApiStatus.Internal
data class CLionBuildTarget(
  val noop: Int, /* dummy field to be replaced by actial data*/
) : BuildTargetData

@ApiStatus.Internal
fun extractCLionBuildTarget(target: BuildTarget): CLionBuildTarget? = target.extractData()

