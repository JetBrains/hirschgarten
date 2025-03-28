package org.jetbrains.bazel.server.label

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label

fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.key.label)

fun BspTargetInfo.Dependency.label(): Label = Label.parse(this.id)
