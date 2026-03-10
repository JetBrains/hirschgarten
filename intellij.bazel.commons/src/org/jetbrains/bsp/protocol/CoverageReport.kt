package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class CoverageReport(val taskId: TaskId, val coverageReport: Path)
