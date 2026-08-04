package org.jetbrains.bazel.run.test

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.state.HasTestFilter

/**
 * Allows right-clicking on a test in the test results and then rerunning it separately from other tests.
 *
 * Works with any test runner: when the JetBrains custom runner is in use we re-run by the runner's
 * own test unique ids (via the `JB_TEST_UNIQUE_IDS` env var, see [setTestUniqueIds]); otherwise we
 * re-run via a generic bazel `--test_filter` (see [setTestFilter]) derived from the selected node's
 * location by [BazelTestFilterProvider], matching what the gutter "run test" action produces.
 *
 * @see BazelRerunFailedTestsAction
 * @see com.intellij.execution.junit.UniqueIdConfigurationProducer
 */
private class BazelRerunTestConfigurationProducer : LazyRunConfigurationProducer<BazelRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory =
    ConfigurationTypeUtil.findConfigurationType(BazelRunConfigurationType::class.java).configurationFactories.first()

  override fun setupConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>,
  ): Boolean {
    val handler = configuration.handler ?: return false
    if (context.project.useJetBrainsTestRunner()) {
      val testIds = getTestIdsFromTestConsole(context)
      if (testIds.isEmpty()) return false
      setTestUniqueIds(handler.state, testIds.toList())
    } else {
      val testFilter = getTestFilterFromTestConsole(context) ?: return false
      setTestFilter(context.project, handler.state, testFilter)
    }

    val selectedProxy = context.dataContext.getData(AbstractTestProxy.DATA_KEY)
    if (selectedProxy != null) {
      val configurationName = getConfigurationName(selectedProxy)
      if (configurationName != null) {
        configuration.name = configurationName
      }
    }

    return true
  }

  override fun isConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
  ): Boolean {
    val state = configuration.handler?.state ?: return false
    return if (context.project.useJetBrainsTestRunner()) {
      val testIds = getTestUniqueIds(state) ?: return false
      testIds.isNotEmpty() && getTestIdsFromTestConsole(context) == testIds
    } else {
      val currentFilter = (state as? HasTestFilter)?.testFilter ?: return false
      currentFilter.isNotEmpty() && getTestFilterFromTestConsole(context) == currentFilter
    }
  }

  private fun getTestIdsFromTestConsole(context: ConfigurationContext): List<String> =
    context.dataContext.getData(AbstractTestProxy.DATA_KEYS).orEmpty().toList().getTestIds()

  /**
   * A bazel `--test_filter` selecting exactly the test node(s) currently selected in the results
   * tree, using the same per-language formatting the gutter uses (see [BazelTestFilterProvider]).
   * Multiple selected nodes are combined into a single alternation regex. Returns null if no
   * selected node has a location we can turn into a filter.
   */
  private fun getTestFilterFromTestConsole(context: ConfigurationContext): String? {
    val filters =
      context.dataContext
        .getData(AbstractTestProxy.DATA_KEYS)
        .orEmpty()
        .mapNotNull { it.locationUrl?.let(BazelTestFilterProvider::testFilterFor) }
        .distinct()
    return filters.takeIf { it.isNotEmpty() }?.joinToString("|")
  }

  private fun getConfigurationName(proxy: AbstractTestProxy): String? {
    // For a URL like java:test://com.example.TestClass/testMethod we will return testMethod
    val locationUrl = proxy.locationUrl ?: return proxy.name
    val slashIndex = locationUrl.lastIndexOf('/')
    if (slashIndex == -1) return proxy.name
    return locationUrl.substring(slashIndex + 1)
  }
}
