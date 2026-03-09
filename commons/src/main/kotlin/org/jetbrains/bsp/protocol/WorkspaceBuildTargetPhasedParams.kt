package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

// TODO: https://youtrack.jetbrains.com/issue/BAZEL-1548
@ApiStatus.Internal
data class WorkspaceBuildTargetPhasedParams(val taskId: TaskId)
