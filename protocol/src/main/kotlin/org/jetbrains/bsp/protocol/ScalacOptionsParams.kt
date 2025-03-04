package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class ScalacOptionsParams(val targets: List<Label>)
