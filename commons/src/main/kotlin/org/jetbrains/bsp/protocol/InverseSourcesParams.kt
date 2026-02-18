package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class InverseSourcesParams(val taskId: TaskId, val files: List<Path>)
