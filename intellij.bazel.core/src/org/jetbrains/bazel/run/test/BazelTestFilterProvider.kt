package org.jetbrains.bazel.run.test

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelTestFilterProvider {
  /**
   * @param locationUrl see [com.intellij.execution.testframework.AbstractTestProxy.getLocationUrl], e.g. `java:test://com.example.FooTest/it_works`.
   * @return the `--test_filter` value that selects exactly that test, or `null` if this provider does not handle the given location URL.
   */
  fun testFilterFromLocationUrl(locationUrl: String): String?

  companion object {
    val ep: ExtensionPointName<BazelTestFilterProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.testFilterProvider")

    fun testFilterFor(locationUrl: String): String? =
      ep.extensionList.firstNotNullOfOrNull { it.testFilterFromLocationUrl(locationUrl) }
  }
}
