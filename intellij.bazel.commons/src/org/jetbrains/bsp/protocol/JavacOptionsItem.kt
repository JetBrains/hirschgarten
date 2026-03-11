package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

internal data class JavacOptionsItem(val target: Label, val options: List<String>)
