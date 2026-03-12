package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

internal data class DependencySourcesParams(val targets: List<Label>)
