package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class InverseSourcesParams(val taskId: TaskId, val files: List<Path>)
