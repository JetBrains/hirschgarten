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
internal class RunSyntheticTargetAction(
  private val project: Project,
  private val target: ExecutableTarget,
  executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
  private val templateGenerator: SyntheticRunTargetTemplateGenerator,
  private val targetElement: PsiElement,
) : BaseRunnerAction(project, executor = executor, configurationName = target.id.toShortString(project)) {

  override suspend fun getRunnerSettings(): RunnerAndConfigurationSettings {
    val configurationType = runConfigurationType<BazelRunConfigurationType>()
    val factory = configurationType.configurationFactories.first()
    val name = templateGenerator.getRunnerActionName(configurationName, target, targetElement)
    val settings =
      RunManager.getInstance(project).createConfiguration(name, factory)
    val config = settings.configuration as BazelRunConfiguration

    val syntheticTargetId = templateGenerator.getSyntheticTargetLabel(target, targetElement)

    // this runner is inferred from the original target
    val originalTargetProvider = RunHandlerProvider.getRunHandlerProvider(listOf(target.kind))
      ?: error("Failed to get run provider")
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
