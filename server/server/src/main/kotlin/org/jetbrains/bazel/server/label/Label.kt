package org.jetbrains.bazel.server.label

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.id)

fun BspTargetInfo.Dependency.label(): Label = Label.parse(this.id)
