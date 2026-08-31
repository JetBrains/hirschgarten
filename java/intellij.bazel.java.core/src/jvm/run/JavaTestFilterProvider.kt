package org.jetbrains.bazel.jvm.run

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.java.ui.gutters.getTestFilter
import org.jetbrains.bazel.run.test.BazelTestFilterProvider
import org.jetbrains.bazel.testing.TEST_CASE_PREFIX
import org.jetbrains.bazel.testing.TEST_SUITE_PREFIX

@ApiStatus.Internal
class JavaTestFilterProvider : BazelTestFilterProvider {
  /**
   * Reads back a location hint written by [org.jetbrains.bazel.testing.BazelJavaTestLocationHintProvider],
   * e.g., `java:suite://com.example.Outer$Inner` or `java:test://com.example.Outer$Inner/testName`, and turns it into a test filter.
   * @see org.jetbrains.bazel.java.ui.gutters.BazelJavaRunConfigurationProducer.getGutterAction
   */
  override fun testFilterFromLocationUrl(locationUrl: String): String? {
    val body =
      when {
        locationUrl.startsWith(TEST_SUITE_PREFIX) -> locationUrl.removePrefix(TEST_SUITE_PREFIX)
        locationUrl.startsWith(TEST_CASE_PREFIX) -> locationUrl.removePrefix(TEST_CASE_PREFIX)
        else -> return null
      }
    val classAndMethod = body.split("/")
    return getTestFilter(className = classAndMethod[0], methodName = classAndMethod.getOrNull(1))
  }
}
