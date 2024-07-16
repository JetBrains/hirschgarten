package org.jetbrains.bsp.protocol

public data class JUnitStyleTestCaseData (
        val time: Double,
        val className: String?,
        val pkg: String?,
        val fullError: String?,
        val errorType: String?,
) {
    companion object {
        const val DATA_KIND = "junit-style-test-case-data"
    }
}
