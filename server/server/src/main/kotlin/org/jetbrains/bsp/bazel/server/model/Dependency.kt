package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bazel.commons.label.Label

data class Dependency(val label: Label, val exported: Boolean)
