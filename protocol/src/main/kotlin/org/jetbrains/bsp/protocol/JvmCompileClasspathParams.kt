package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class JvmCompileClasspathParams(val targets: List<Label>)
