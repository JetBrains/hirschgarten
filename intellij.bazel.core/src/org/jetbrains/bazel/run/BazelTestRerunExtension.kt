package org.jetbrains.bazel.run

import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter

@ApiStatus.Internal
interface BazelTestRerunExtension {
  fun isApplicable(configuration: BazelRunConfiguration): Boolean

  fun setTestsToRerun(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): Boolean

  fun isRerunOf(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): Boolean

  companion object {
    val ep: ExtensionPointName<BazelTestRerunExtension> = ExtensionPointName("org.jetbrains.bazel.testRerunExtension")

    fun getInstance(configuration: BazelRunConfiguration): BazelTestRerunExtension? = ep.findFirstSafe { it.isApplicable(configuration) }
  }
}

@ApiStatus.Internal
abstract class BazelTestFilterRerunExtension : BazelTestRerunExtension {
  final override fun setTestsToRerun(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): Boolean {
    val state = configuration.handler?.state ?: return false
    (state as? HasTestFilter)?.testFilter = getTestFilter(configuration, tests) ?: return false
    return true
  }

  final override fun isRerunOf(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): Boolean {
    val state = configuration.handler?.state ?: return false
    val testFilter = getTestFilter(configuration, tests) ?: return false
    return testFilter == (state as? HasTestFilter)?.testFilter
  }

  abstract fun getTestFilter(configuration: BazelRunConfiguration, tests: List<AbstractTestProxy>): String?
}
