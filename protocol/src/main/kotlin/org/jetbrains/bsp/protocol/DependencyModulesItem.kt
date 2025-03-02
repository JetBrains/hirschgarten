package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class DependencyModulesItem(val target: Label, val modules: List<DependencyModule>)
