package org.jetbrains.bazel.runnerAction

import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.synthetic.GENERATE_SYNTHETIC_PROVIDER_ID
import org.jetbrains.bazel.run.synthetic.GenerateSyntheticTargetRunTaskProvider
import org.jetbrains.bazel.run.synthetic.SyntheticRunTargetTemplateGenerator
import org.jetbrains.bsp.protocol.ExecutableTarget

// TODO: refactor to Execution API
private fun getSyntheticConfigurationName(
  project: Project,
  target: ExecutableTarget,
  templateGenerator: SyntheticRunTargetTemplateGenerator,
  targetElement: PsiElement,
): String = templateGenerator.getRunnerActionName(target.id.toShortString(project), target, targetElement)

internal class RunSyntheticTargetAction(
  private val project: Project,
  private val target: ExecutableTarget,
  executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
  private val templateGenerator: SyntheticRunTargetTemplateGenerator,
  private val targetElement: PsiElement,
  configurationName: String = getSyntheticConfigurationName(project, target, templateGenerator, targetElement),
) : BaseRunnerAction(project, executor = executor, configurationName = configurationName) {

  override suspend fun getRunnerSettings(): RunnerAndConfigurationSettings? = createRunConfiguration()

  fun createRunConfiguration(): RunnerAndConfigurationSettings? =
    createRunConfiguration(
      project = project,
      target = target,
      templateGenerator = templateGenerator,
      targetElement = targetElement,
      configurationName = configurationName,
    )

  companion object {
    fun createRunConfiguration(
      project: Project,
      target: ExecutableTarget,
      templateGenerator: SyntheticRunTargetTemplateGenerator,
      targetElement: PsiElement,
      configurationName: String = getSyntheticConfigurationName(project, target, templateGenerator, targetElement),
    ): RunnerAndConfigurationSettings? {
      val configurationType = runConfigurationType<BazelRunConfigurationType>()
      val factory = configurationType.configurationFactories.first()
      val settings =
        RunManager.getInstance(project).createConfiguration(configurationName, factory)
      val config = settings.configuration as BazelRunConfiguration

      val syntheticTargetId = templateGenerator.getSyntheticTargetLabel(target, targetElement)

      // this runner is inferred from the original target
      val originalTargetProvider = RunHandlerProvider.getRunHandlerProvider(listOf(target.kind))
        ?: return null
      config.updateRunProvider(listOf(syntheticTargetId), originalTargetProvider)

      val provider = BeforeRunTaskProvider.getProvider(project, GENERATE_SYNTHETIC_PROVIDER_ID)
      if (provider is GenerateSyntheticTargetRunTaskProvider) {
        val params = templateGenerator.getSyntheticParams(target, targetElement)
        val task = provider.createTask(config)
        val taskState = task.taskState
        taskState.target = target.id.toString()
        taskState.language = targetElement.language.id
        taskState.params = params.data
        settings.configuration.beforeRunTasks = listOf(task) + settings.configuration.beforeRunTasks
      }

      settings.isTemporary = true
      return settings
    }
  }
}
