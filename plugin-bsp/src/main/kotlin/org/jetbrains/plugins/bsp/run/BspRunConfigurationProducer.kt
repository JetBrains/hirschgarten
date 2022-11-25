package org.jetbrains.plugins.bsp.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

public class BspRunConfigurationProducer : LazyRunConfigurationProducer<BspRunConfiguration>() {
  override fun isConfigurationFromContext(configuration: BspRunConfiguration, context: ConfigurationContext): Boolean {
    return false
  }

  override fun setupConfigurationFromContext(
    configuration: BspRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>
  ): Boolean {
    return false
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return BspRunConfigurationType.getInstance().factory
  }
}
