package org.jetbrains.bsp.protocol

import org.jetbrains.bsp.protocol.TestParams

data class TestWithDebugParams(val originId: String, val testParams: TestParams)
