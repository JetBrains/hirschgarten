package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel

data class JavacOptionsParams(val targets: List<CanonicalLabel>)
