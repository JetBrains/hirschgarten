package org.jetbrains.bazel.jvm.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.run.test.BazelTestFilterProvider

/**
 * Allows right-clicking on a test in the test results and then rerunning it separately from other tests
 * @see BazelRerunFailedTestsAction
 * @see com.intellij.execution.junit.UniqueIdConfigurationProducer
 */
internal class BazelRerunTestConfigurationProducer : LazyRunConfigurationProducer<BazelRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory = bazelRunConfigurationFactory

  override fun setupConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>,
  ): Boolean {
    val handler = configuration.handler ?: return false
    if (configuration.targetsUseJetBrainsTestRunner()) {
      val testIds = getTestIdsFromTestConsole(context)
      if (testIds.isEmpty()) return false
      JetBrainsTestRunner.setTestUniqueIds(handler.state, testIds.toList())
    }
    else {
      val testFilter = getTestFilterFromTestConsole(context) ?: return false
      (handler.state as? AbstractGenericTestState<*>)?.testFilter = testFilter
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
    return if (configuration.targetsUseJetBrainsTestRunner()) {
      val testIds = JetBrainsTestRunner.getTestUniqueIds(state) ?: return false
      testIds.isNotEmpty() && getTestIdsFromTestConsole(context) == testIds
    }
    else {
      getTestFilterFromTestConsole(context) == (state as? HasTestFilter)?.testFilter
    }
  }

  private fun getTestIdsFromTestConsole(context: ConfigurationContext): List<String> =
    context.dataContext.getData(AbstractTestProxy.DATA_KEYS).orEmpty().toList().getTestIds()

  private fun getTestFilterFromTestConsole(context: ConfigurationContext): String? {
    val filters =
      context.dataContext
        .getData(AbstractTestProxy.DATA_KEYS)
        .orEmpty()
        .mapNotNull { it.locationUrl?.let(BazelTestFilterProvider::testFilterFor) }
        .distinct()
    return filters.joinToString("|")
  }

  private fun getConfigurationName(proxy: AbstractTestProxy): String? {
    // For a URL like java:test://com.example.TestClass/testMethod we will return testMethod
    val locationUrl = proxy.locationUrl ?: return proxy.name
    val slashIndex = locationUrl.lastIndexOf('/')
    if (slashIndex == -1) return proxy.name
    return locationUrl.substring(slashIndex + 1)
  }
}
