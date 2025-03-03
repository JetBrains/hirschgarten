package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class ScalaTestClassesParams(val targets: List<Label>, val originId: String? = null)
