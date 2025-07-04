package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label

data class DependencySourcesParams(val targets: List<CanonicalLabel>)
