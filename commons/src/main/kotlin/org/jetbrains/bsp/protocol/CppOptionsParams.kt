package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel

data class CppOptionsParams(val targets: List<CanonicalLabel>)
