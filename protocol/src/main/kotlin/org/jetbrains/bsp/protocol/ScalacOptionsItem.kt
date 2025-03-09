package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class ScalacOptionsItem(val target: Label, val options: List<String>)
