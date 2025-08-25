package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class WorkspaceTargetClasspathQueryParams(val target: Label)
