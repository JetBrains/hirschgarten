package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelStatus

data class RunResult(val taskId: TaskId, val statusCode: BazelStatus)
