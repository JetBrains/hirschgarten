package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class BuildTargetEvent(val target: Label, val kind: BuildTargetEventKind? = null)
