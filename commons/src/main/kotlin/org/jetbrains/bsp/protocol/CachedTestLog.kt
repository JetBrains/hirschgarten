package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class CachedTestLog(val taskId: TaskId, val testLog: Path)
