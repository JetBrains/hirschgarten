package org.jetbrains.bazel.jvm.run

import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.run.test.BazelTestFilterProvider

@ApiStatus.Internal
interface JvmTestRerunExtension {
  fun isApplicable(configuration: BazelRunConfiguration): Boolean

  fun setTestsToRerun(state: BazelRunConfigurationState<*>, tests: List<AbstractTestProxy>): Boolean

  fun isRerunOf(state: BazelRunConfigurationState<*>, tests: List<AbstractTestProxy>): Boolean

  companion object {
    val ep: ExtensionPointName<JvmTestRerunExtension> = ExtensionPointName("org.jetbrains.bazel.jvmTestRerunExtension")

    fun getInstance(configuration: BazelRunConfiguration): JvmTestRerunExtension =
      checkNotNull(ep.findFirstSafe { it.isApplicable(configuration) })
  }
}

internal class DefaultJvmTestRerunExtension : JvmTestRerunExtension {
  override fun isApplicable(configuration: BazelRunConfiguration): Boolean = true

  override fun setTestsToRerun(state: BazelRunConfigurationState<*>, tests: List<AbstractTestProxy>): Boolean {
    (state as? HasTestFilter)?.testFilter = testFilterFromTests(tests) ?: return false
    return true
  }

  override fun isRerunOf(state: BazelRunConfigurationState<*>, tests: List<AbstractTestProxy>): Boolean {
    val testFilter = testFilterFromTests(tests) ?: return false
    return testFilter == (state as? HasTestFilter)?.testFilter
  }
}

@ApiStatus.Internal
fun testFilterFromTests(tests: List<AbstractTestProxy>): String? =
  tests
    .mapNotNull { it.locationUrl?.let(BazelTestFilterProvider::testFilterFor) }
    .distinct()
    .joinToString("|")
    .ifEmpty { null }
