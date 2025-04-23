package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class WorkspaceBuildTargetsPartialParams(val targets: List<Label>)
