package org.jetbrains.bazel.run.test

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus

/**
 * Language-specific strategy for turning a test-tree node's location URL (as produced by
 * [org.jetbrains.bazel.testing.BazelTestLocationHintProvider]) into a bazel `--test_filter` value.
 *
 * This lets a single test be re-run from the test results tree (right-click -> "Run") with *any*
 * test runner, not only the JetBrains custom runner: the JetBrains runner is driven by the
 * `JB_TEST_UNIQUE_IDS` environment variable (see [setTestUniqueIds]), which no other runner
 * understands, whereas `--test_filter` (see [setTestFilter]) is a generic bazel flag honored by
 * the native JUnit runner and by custom runners alike.
 *
 * The returned value must match, byte for byte, the `--test_filter` that the language's gutter
 * "run test" action produces for the same test, so that re-running from the gutter and from the
 * results-tree context menu behave identically.
 */
@ApiStatus.Internal
interface BazelTestFilterProvider {
  /**
   * @param locationUrl the [com.intellij.execution.testframework.AbstractTestProxy.getLocationUrl]
   *   of a selected test node, e.g. `java:test://com.example.FooTest/it_works`.
   * @return the `--test_filter` value that selects exactly that test, or `null` if this provider
   *   does not handle the given location URL.
   */
  fun testFilterFromLocationUrl(locationUrl: String): String?

  companion object {
    val ep: ExtensionPointName<BazelTestFilterProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.testFilterProvider")

    /** The first non-null filter produced by any registered provider, or `null` if none applies. */
    fun testFilterFor(locationUrl: String): String? =
      ep.extensionList.firstNotNullOfOrNull { it.testFilterFromLocationUrl(locationUrl) }
  }
}
