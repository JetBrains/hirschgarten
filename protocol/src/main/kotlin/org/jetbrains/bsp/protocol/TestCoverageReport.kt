package org.jetbrains.bsp.protocol

public data class TestCoverageReport(val lcovReportUri: String) {
  companion object {
    const val DATA_KIND = "coverage-report"
  }
}
