package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel

data class DependencySourcesParams(val targets: List<CanonicalLabel>)
