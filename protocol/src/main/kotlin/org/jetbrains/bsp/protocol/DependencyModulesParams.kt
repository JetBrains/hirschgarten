package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class DependencyModulesParams(val targets: List<Label>)
