package org.jetbrains.plugins.bsp.gdb

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.config.BspRunConfigurationType

class GTestRunConfigurationProvider<T: RunConfiguration>( ): RunConfigurationProducer<T>(
  ConfigurationTypeUtil.findConfigurationType(BspRunConfigurationType::class.java) as ConfigurationType,
) {

    val tp=RadGTestContextProvider()
    override fun setupConfigurationFromContext(cfg: T, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
      val a=cfg.toString()
      println(a)
      val configuration=cfg as BspRunConfiguration
      val testContext=tp.getTestContext(context) ?: return false
      testContext.setupRunConfiguration(configuration)
      sourceElement.set(testContext.sourceElement)
      return true

    }

    override fun isConfigurationFromContext(cfg: T, context: ConfigurationContext): Boolean {
      return false
      val configuration=cfg as BspRunConfiguration
      val testContext=tp.getTestContext(context) ?: return false
      return testContext.setupRunConfiguration(configuration)
    }

  override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?): Boolean {
    return if(self?.configuration is BspRunConfiguration){
      true
    }else if(other?.configuration is BspRunConfiguration){
      false
    }else{
      true
    }
  }

  override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
    return (self?.configuration is BspRunConfiguration)&& other.configuration !is BspRunConfiguration
  }


  }
