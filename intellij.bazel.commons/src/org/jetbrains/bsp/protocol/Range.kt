package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class Range(val start: Position, val end: Position)
