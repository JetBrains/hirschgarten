package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class WorkspaceTargetClasspathQueryParams(val target: Label)
