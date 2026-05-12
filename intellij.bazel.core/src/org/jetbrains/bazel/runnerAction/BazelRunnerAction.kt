package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
abstract class BazelRunnerAction(
  private val project: Project,
  private val targets: List<ExecutableTarget>,
  executor: Executor,
  configurationName: String,
  private val callerPsiElement: PsiElement? = null,
) : BaseRunnerAction(executor, configurationName) {

  private fun getConfigurationType(): ConfigurationType = runConfigurationType<BazelRunConfigurationType>()

  protected open fun RunnerAndConfigurationSettings.customizeRunConfiguration() {}

  override suspend fun getRunnerSettings(): RunnerAndConfigurationSettings? {
    return createRunConfiguration()
  }

  fun createRunConfiguration(): RunnerAndConfigurationSettings {
    val factory = getConfigurationType().configurationFactories.first()
    val settings =
      RunManager.getInstance(project).createConfiguration(configurationName, factory)
    val configuration = settings.configuration as BazelRunConfiguration
    configuration.updateTargets(targets.map { it.id }, RunHandlerProvider.getRunHandlerProvider(targets.map { it.kind }))
    settings.customizeRunConfiguration()

    if (callerPsiElement != null) {
      val location = ReadAction.nonBlocking<PsiLocation<PsiElement>> { PsiLocation(callerPsiElement) }.executeSynchronously()
      configuration.handler?.extensionsManager?.extendCreatedConfiguration(configuration, location)
    }
    return settings
  }
}
