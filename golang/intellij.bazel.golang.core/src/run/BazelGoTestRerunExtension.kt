package org.jetbrains.bazel.golang.run

import com.intellij.execution.testframework.AbstractTestProxy
import org.jetbrains.bazel.golang.debug.BazelGoTestHandler
import org.jetbrains.bazel.run.BazelTestFilterRerunExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class BazelGoTestRerunExtension : BazelTestFilterRerunExtension() {
  override fun isApplicable(configuration: BazelRunConfiguration): Boolean =
    configuration.handler is BazelGoTestHandler

  override fun getTestFilter(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): String? =
    tests
      .mapNotNull { it.locationUrl }
      .mapNotNull { getTestName(it) }
      .distinct()
      .joinToString("|") { regexifyTestFilter(it) }
      .takeIf { it.isNotEmpty() }

  /**
   * @see BazelGoTestLocationHintProvider
   */
  private fun getTestName(locationUrl: String): String? =
    locationUrl.substringAfter('#', "").substringBefore('/').takeIf { it.isNotEmpty() }
}
