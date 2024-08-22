package org.jetbrains.bsp.protocol

public data class JUnitStyleTestCaseData(
  val time: Double?,
  val className: String?,
  val errorMessage: String?,
  val errorContent: String?,
  val errorType: String?,
) {
  companion object {
    const val DATA_KIND = "junit-style-test-case-data"
  }
}

public data class JUnitStyleTestSuiteData(
  val time: Double?,
  val systemOut: String?,
  val systemErr: String?,
) {
  companion object {
    const val DATA_KIND = "junit-style-test-suite-data"
  }
}
