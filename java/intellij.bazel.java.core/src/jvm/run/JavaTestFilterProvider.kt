package org.jetbrains.bazel.jvm.run

import org.jetbrains.bazel.run.test.BazelTestFilterProvider
import org.jetbrains.bazel.testing.FRAGMENT_DELIMITER
import org.jetbrains.bazel.testing.PROTOCOL_DELIMITER
import org.jetbrains.bazel.testing.SUITE_DELIMITER
import org.jetbrains.bazel.testing.TEST_CASE_PROTOCOL
import org.jetbrains.bazel.testing.TEST_SUITE_PROTOCOL

internal class JavaTestFilterProvider : BazelTestFilterProvider {
  /**
   * Reads back a location hint written by [org.jetbrains.bazel.testing.BazelJavaTestLocationHintProvider]
   * -- `java:suite://com.example.Outer${'$'}Inner` or `java:test://com.example.Outer${'$'}Inner/testName` --
   * and turns it into the filter a gutter run would produce, so that re-running from the results
   * tree selects exactly what re-running from the editor would. See
   * [org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor.getSingleTestFilter].
   */
  override fun testFilterFromLocationUrl(locationUrl: String): String? {
    val suitePrefix = "$TEST_SUITE_PROTOCOL$PROTOCOL_DELIMITER"
    val casePrefix = "$TEST_CASE_PROTOCOL$PROTOCOL_DELIMITER"
    val body =
      when {
        locationUrl.startsWith(suitePrefix) -> locationUrl.removePrefix(suitePrefix)
        locationUrl.startsWith(casePrefix) -> locationUrl.removePrefix(casePrefix)
        else -> return null
      }

    // A nested class is separated with '$', which would be read as a regex end-of-input anchor once
    // bazel matches the filter, so use '.' instead. The class name never contains the fragment
    // delimiter -- the hint provider replaces any it finds -- so everything past the first one is
    // the test name.
    val className = body.substringBefore(FRAGMENT_DELIMITER).replace(SUITE_DELIMITER, ".")
    if (className.isEmpty()) return null
    val testName = body.substringAfter(FRAGMENT_DELIMITER, missingDelimiterValue = "")
    if (testName.isEmpty()) return className

    // The trailing '$' anchors the test name, so filtering for `it_fails` does not also run
    // `it_fails_again`.
    return "$className.$testName$"
  }
}
