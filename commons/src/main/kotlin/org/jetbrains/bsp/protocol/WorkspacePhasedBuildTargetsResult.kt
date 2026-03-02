package org.jetbrains.bsp.protocol

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@JvmInline
@ApiStatus.Internal
value class RawPhasedTarget(val target: Build.Target)

@ApiStatus.Internal
data class WorkspacePhasedBuildTargetsResult(val targets: Map<Label, RawPhasedTarget>)
