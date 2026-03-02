package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

internal data class JavacOptionsParams(val targets: List<Label>)
