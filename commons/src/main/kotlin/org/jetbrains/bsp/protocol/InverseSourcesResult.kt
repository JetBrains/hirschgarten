package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel

data class InverseSourcesResult(val targets: List<CanonicalLabel>)
