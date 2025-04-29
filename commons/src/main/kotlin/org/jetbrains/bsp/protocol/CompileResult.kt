package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelStatus

data class CompileResult(val statusCode: BazelStatus)
