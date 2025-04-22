package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class PythonOptionsParams(val targets: List<Label>)
