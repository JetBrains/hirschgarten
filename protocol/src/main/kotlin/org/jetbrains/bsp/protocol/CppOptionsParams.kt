package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class CppOptionsParams(val targets: List<Label>)
