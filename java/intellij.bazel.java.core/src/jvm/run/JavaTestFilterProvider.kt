package org.jetbrains.bazel.jvm.run

import org.jetbrains.bazel.run.test.BazelTestFilterProvider
import org.jetbrains.bazel.testing.BazelTestLocationHintProvider

/**
 * The bazel `--test_filter` value for running a single Java test method when the JetBrains test
 * runner is *not* in use. Shared with [org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor]
 * so that the gutter "run test" action and the results-tree context menu produce identical filters.
 *
 * The trailing `$` anchors the method name so that filtering for `it_fails` does not also match a
 * sibling `it_fails_again` (matched as a regex by bazel's `--test_filter`).
 */
internal fun javaMethodTestFilter(simpleClassName: String, methodName: String?): String = "$simpleClassName.$methodName$"

internal class JavaTestFilterProvider : BazelTestFilterProvider {
  override fun testFilterFromLocationUrl(locationUrl: String): String? {
    val isCase = locationUrl.startsWith("${BazelTestLocationHintProvider.TEST_CASE_PROTOCOL}://")
    val isSuite = locationUrl.startsWith("${BazelTestLocationHintProvider.TEST_SUITE_PROTOCOL}://")
    if (!isCase && !isSuite) return null

    val hint = BazelTestLocationHintProvider.parseLocationHint(locationUrl)
    val suites = hint.classNameOrSuites.filter { it.isNotBlank() }
    if (suites.isEmpty()) return null
    val methodName = hint.methodName.ifBlank { null }

    return if (methodName == null) {
      // Whole-class rerun: mirror the gutter, which emits the JVM class name (nested classes are
      // joined with '$').
      suites.joinToString("\$")
    } else {
      // Single-method rerun: mirror the gutter, which uses the *simple* class name.
      val simpleClassName = suites.last().substringAfterLast('.')
      javaMethodTestFilter(simpleClassName, methodName)
    }
  }
}
