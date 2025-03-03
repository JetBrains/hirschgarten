package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class JvmCompileClasspathItem(val target: Label, val classpath: List<String>)
