package org.jetbrains.bsp.protocol

import java.nio.file.Path

data class CoverageReport(val originId: String, val coverageReport: Path)
