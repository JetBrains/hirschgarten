package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class CachedTestLog(val taskId: TaskId, val testLog: Path)
