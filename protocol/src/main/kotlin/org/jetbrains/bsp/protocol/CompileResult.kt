package org.jetbrains.bsp.protocol

data class CompileResult(val originId: String? = null, val statusCode: StatusCode)
