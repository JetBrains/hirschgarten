package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class InverseSourcesResult(val targets: List<Label>)
