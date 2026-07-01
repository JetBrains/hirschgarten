package org.jetbrains.bazel.protobuf.target

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import org.jetbrains.bsp.protocol.extractData

@ClassDiscriminator(9)
@ApiStatus.Internal
data class ProtobufBuildTarget(
  val sources: Map<String, String>, // import path -> real file
) : BuildTargetData

@ApiStatus.Internal
fun extractProtobufBuildTarget(target: BuildTarget): ProtobufBuildTarget? = target.extractData()
