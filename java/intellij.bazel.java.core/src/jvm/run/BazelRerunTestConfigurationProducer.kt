package org.jetbrains.bazel.jvm.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory

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
    if (!configuration.targetsUseJetBrainsTestRunner()) return false
    val testIds = getTestIdsFromTestConsole(context)
    if (testIds.isEmpty()) return false
    val handler = configuration.handler ?: return false
    JetBrainsTestRunner.setTestUniqueIds(handler.state, testIds.toList())

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
    if (!configuration.targetsUseJetBrainsTestRunner()) return false
    val state = configuration.handler?.state ?: return false
    val testIds = JetBrainsTestRunner.getTestUniqueIds(state) ?: return false
    return testIds.isNotEmpty() && getTestIdsFromTestConsole(context) == testIds
  }

  private fun getTestIdsFromTestConsole(context: ConfigurationContext): List<String> =
    context.dataContext.getData(AbstractTestProxy.DATA_KEYS).orEmpty().toList().getTestIds()

  private fun getConfigurationName(proxy: AbstractTestProxy): String? {
    // For a URL like java:test://com.example.TestClass/testMethod we will return testMethod
    val locationUrl = proxy.locationUrl ?: return proxy.name
    val slashIndex = locationUrl.lastIndexOf('/')
    if (slashIndex == -1) return proxy.name
    return locationUrl.substring(slashIndex + 1)
  }
}
