package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class RustWorkspaceParams(val targets: List<Label>)
