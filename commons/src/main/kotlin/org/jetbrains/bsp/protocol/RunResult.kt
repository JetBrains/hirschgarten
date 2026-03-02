package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus

@ApiStatus.Internal
data class RunResult(val taskId: TaskId, val statusCode: BazelStatus)
