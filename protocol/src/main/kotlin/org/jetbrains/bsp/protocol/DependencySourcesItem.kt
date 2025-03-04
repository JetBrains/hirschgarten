package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class DependencySourcesItem(val target: Label, val sources: List<String>)
