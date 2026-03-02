package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class JvmMainClass(val className: String, val arguments: List<String>)
