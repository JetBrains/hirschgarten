package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class CoverageReport(val taskId: TaskId, val coverageReport: Path)
