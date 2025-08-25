package org.jetbrains.bsp.protocol

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.bazel.label.Label

@JvmInline
value class RawPhasedTarget(val target: Build.Target)

data class WorkspacePhasedBuildTargetsResult(val targets: Map<Label, RawPhasedTarget>)
