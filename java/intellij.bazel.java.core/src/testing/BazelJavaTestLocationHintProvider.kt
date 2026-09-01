package org.jetbrains.bazel.testing

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.includesJava
import org.jetbrains.bazel.util.BazelTestLocationHintProvider

@ApiStatus.Internal
class BazelJavaTestLocationHintProvider : BazelTestLocationHintProvider {
  override fun isApplicable(targetKind: TargetKind): Boolean = targetKind.includesJava()

  override fun getLocationHint(testDetails: BazelTestDetails): String =
    testDetails.run {
      if (isSuite) {
        testSuiteLocationHint(displayName, classname, parentSuiteNames)
      } else {
        testCaseLocationHint(displayName, classname, parentSuiteNames)
      }
    }

  /**
   * Generates a location hint for a test case.
   * If non-null `classname` is given, `parentSuites` is ignored - it serves as a fallback if `classname` is unknown
   */
  fun testCaseLocationHint(
    testName: String,
    classname: String? = null,
    parentSuites: List<String> = emptyList(),
  ): String {
    val classnameToUse = classname ?: parentSuites.joinToString(SUITE_DELIMITER)
    val cleanClassName = classnameToUse.removeMisleadingDelimiters()
    val cleanTestName = testName.removeSuffix("()")
    return "$TEST_CASE_PROTOCOL$PROTOCOL_DELIMITER$cleanClassName$FRAGMENT_DELIMITER$cleanTestName"
  }

  /**
   * Generates a location hint for a test suite.
   * If non-null `classname` is given, `parentSuites` is ignored - it serves as a fallback if `classname` is unknown
   */
  fun testSuiteLocationHint(
    suiteName: String,
    classname: String? = null,
    parentSuites: List<String> = emptyList(),
  ): String {
    val classnameToUse =
      when {
        classname != null -> classname
        parentSuites.isEmpty() -> suiteName
        else -> {
          val generatedClassname = parentSuites.joinToString(SUITE_DELIMITER, postfix = "$SUITE_DELIMITER$suiteName")
          generatedClassname
        }
      }
    val cleanClassName = classnameToUse.removeMisleadingDelimiters()
    return "$TEST_SUITE_PROTOCOL$PROTOCOL_DELIMITER$cleanClassName"
  }

  private fun String.removeMisleadingDelimiters(): String =
    replace(FRAGMENT_DELIMITER, SUITE_DELIMITER) // otherwise the delimiters might get misinterpreted

  companion object {
    fun getInstance(): BazelJavaTestLocationHintProvider =
      checkNotNull(BazelTestLocationHintProvider.ep.findExtension(BazelJavaTestLocationHintProvider::class.java))
  }
}

private const val TEST_CASE_PROTOCOL: String = "java:test"
private const val TEST_SUITE_PROTOCOL: String = "java:suite"
private const val PROTOCOL_DELIMITER = "://"
private const val SUITE_DELIMITER = "$"
private const val FRAGMENT_DELIMITER = "/"
