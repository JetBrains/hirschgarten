package org.jetbrains.bazel.protobuf.target

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.extractData

@ApiStatus.Internal
data class ProtobufBuildTarget(
  val sources: Map<String, OutputLocation>, // import path -> proto file
) : BuildTargetData

@ApiStatus.Internal
fun extractProtobufBuildTarget(target: BuildTarget): ProtobufBuildTarget? = target.extractData()
