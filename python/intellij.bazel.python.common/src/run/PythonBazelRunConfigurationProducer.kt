package org.jetbrains.bazel.python.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.run.test.setTestFilter

internal class PythonBazelRunConfigurationProducer : LazyRunConfigurationProducer<BazelRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory =
    ConfigurationTypeUtil.findConfigurationType(BazelRunConfigurationType::class.java).configurationFactories.first()

  override fun setupConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>,
  ): Boolean {
    val runContext = context.findRunContext() ?: return false
    if (!configuration.applyRunContext(runContext)) return false
    sourceElement.set(runContext.sourceElement)
    return true
  }

  override fun isConfigurationFromContext(configuration: BazelRunConfiguration, context: ConfigurationContext): Boolean {
    val runContext = context.findRunContext() ?: return false
    return configuration.targets == listOf(runContext.target.id) && when (runContext) {
      is PythonBazelRunContext.Binary ->
        configuration.handler?.isTestHandler == false

      is PythonBazelRunContext.Test ->
        configuration.handler?.isTestHandler == true &&
        configuration.programArguments == formatProgramArguments(runContext.testExecutableArguments)
    }
  }

  private fun ConfigurationContext.findRunContext(): PythonBazelRunContext? {
    if (!project.isBazelProject) return null
    val element = psiLocation ?: return null
    return PythonBazelRunUtils.findPythonBazelRunContext(element)
  }

  private fun BazelRunConfiguration.applyRunContext(runContext: PythonBazelRunContext): Boolean {
    val provider = RunHandlerProvider.getRunHandlerProvider(listOf(runContext.target.kind)) ?: return false
    name = runContext.configurationName
    updateTargets(listOf(runContext.target.id), provider)
    val bazelHandler = handler ?: return false

    if (runContext is PythonBazelRunContext.Test) {
      setTestFilter(project, bazelHandler.state, null)
      (bazelHandler.state as? HasProgramArguments)?.programArguments = formatProgramArguments(runContext.testExecutableArguments)
    }
    return true
  }

  private fun formatProgramArguments(arguments: List<String>): String =
    arguments.joinToString(" ") { argument ->
      val escaped = argument.replace("\"", "\\\"")
      "\"$escaped\""
    }

  private val BazelRunConfiguration.programArguments: String?
    get() = (handler?.state as? HasProgramArguments)?.programArguments
}
