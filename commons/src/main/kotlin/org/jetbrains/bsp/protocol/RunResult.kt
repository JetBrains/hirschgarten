package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelStatus

data class RunResult(val originId: String? = null, val statusCode: BazelStatus)
