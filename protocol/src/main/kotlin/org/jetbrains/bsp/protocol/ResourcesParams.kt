package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class ResourcesParams(val targets: List<Label>)
