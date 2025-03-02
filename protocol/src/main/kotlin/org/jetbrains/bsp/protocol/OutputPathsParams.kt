package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class OutputPathsParams(val targets: List<Label>)
