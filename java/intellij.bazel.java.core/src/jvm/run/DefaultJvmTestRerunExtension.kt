package org.jetbrains.bazel.jvm.run

import com.intellij.execution.testframework.AbstractTestProxy
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.java.ui.gutters.getTestFilter
import org.jetbrains.bazel.run.BazelTestFilterRerunExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.testing.TEST_CASE_PREFIX
import org.jetbrains.bazel.testing.TEST_SUITE_PREFIX

internal class DefaultJvmTestRerunExtension : BazelTestFilterRerunExtension() {
  override fun isApplicable(configuration: BazelRunConfiguration): Boolean = configuration.handler is JvmTestHandler

  override fun getTestFilter(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): String? = jvmTestFilterFromTests(tests)
}

@ApiStatus.Internal
fun jvmTestFilterFromTests(tests: List<AbstractTestProxy>): String? =
  tests
    .mapNotNull { it.locationUrl?.let(::jvmTestFilterFromLocationUrl) }
    .distinct()
    .joinToString("|")
    .ifEmpty { null }

/**
 * Reads back a location hint written by [org.jetbrains.bazel.testing.BazelJavaTestLocationHintProvider],
 * e.g., `java:suite://com.example.Outer$Inner` or `java:test://com.example.Outer$Inner/testName`, and turns it into a test filter.
 * @see org.jetbrains.bazel.java.ui.gutters.BazelJavaRunConfigurationProducer.getGutterAction
 */
@ApiStatus.Internal
fun jvmTestFilterFromLocationUrl(locationUrl: String): String? {
  val body =
    when {
      locationUrl.startsWith(TEST_SUITE_PREFIX) -> locationUrl.removePrefix(TEST_SUITE_PREFIX)
      locationUrl.startsWith(TEST_CASE_PREFIX) -> locationUrl.removePrefix(TEST_CASE_PREFIX)
      else -> return null
    }
  val classAndMethod = body.split("/")
  return getTestFilter(className = classAndMethod[0], methodName = classAndMethod.getOrNull(1))
}
