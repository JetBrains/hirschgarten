package org.jetbrains.bazel.server.label

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.key.label)

internal fun BspTargetInfo.Dependency.label(): Label = Label.parse(this.target.label)
