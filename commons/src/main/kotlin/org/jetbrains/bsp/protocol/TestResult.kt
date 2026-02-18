package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelStatus

data class TestResult(val statusCode: BazelStatus)
