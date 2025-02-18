package org.jetbrains.bsp.bazel.server.label

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.info.BspTargetInfo

fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.id)

fun BspTargetInfo.Dependency.label(): Label = Label.parse(this.id)
