package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class OutputPathsItem(val target: Label, val outputPaths: List<OutputPathItem>)
