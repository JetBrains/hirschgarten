package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class SourcesParams(val targets: List<Label>)
