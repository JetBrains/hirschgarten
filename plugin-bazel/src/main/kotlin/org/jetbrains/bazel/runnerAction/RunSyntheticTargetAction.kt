package org.jetbrains.bazel.runnerAction

import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.synthetic.GENERATE_SYNTHETIC_PROVIDER_ID
import org.jetbrains.bazel.run.synthetic.GenerateSyntheticTargetRunTaskProvider
import org.jetbrains.bazel.run.synthetic.SyntheticRunTargetTemplateGenerator
import org.jetbrains.bsp.protocol.BuildTarget

// TODO: refactor to Execution API
open class RunSyntheticTargetAction(
  private val target: BuildTarget,
  private val isDebugAction: Boolean,
  private val includeTargetNameInText: Boolean,
  private val templateGenerator: SyntheticRunTargetTemplateGenerator,
  private val targetElement: PsiElement,
) : BaseRunnerAction(
  text = {
    BazelRunnerActionNaming.getRunActionName(
      isDebugAction = isDebugAction,
      isRunConfigName = false,
      includeTargetNameInText = includeTargetNameInText,
      project = targetElement.project,
      target = target.id,
    )
  },
  isDebugAction = isDebugAction,
  isCoverageAction = false,
) {

  override fun getBuildTargets(project: Project): List<BuildTarget> = listOf(target)

  override suspend fun getRunnerSettings(
    project: Project,
    buildTargets: List<BuildTarget>,
  ): RunnerAndConfigurationSettings {
    val configurationType = runConfigurationType<BazelRunConfigurationType>()
    val factory = configurationType.configurationFactories.first()
    val name = templateGenerator.getRunnerActionName(getRunConfigurationName(), target, targetElement)
    val settings =
      RunManager.getInstance(project).createConfiguration(name, factory)
    val config = settings.configuration as BazelRunConfiguration

    val syntheticTargetIds = buildTargets.map { templateGenerator.getSyntheticTargetLabel(it, targetElement) }

    // this runner is inferred from the original target
    val originalTargetProvider = RunHandlerProvider.getRunHandlerProvider(listOf(target))
      ?: error("Failed to get run provider")
    config.updateRunProvider(syntheticTargetIds, originalTargetProvider)

    val provider = BeforeRunTaskProvider.getProvider(project, GENERATE_SYNTHETIC_PROVIDER_ID)
    if (provider is GenerateSyntheticTargetRunTaskProvider) {
      val params = templateGenerator.getSyntheticParams(target, targetElement)
      val task = provider.createTask(config)
      val taskState = task.taskState
      taskState.target = target.id.toString()
      taskState.language = targetElement.language.id
      taskState.params = params.data
      settings.configuration.beforeRunTasks = listOf(task)
    }

    settings.isTemporary = true
    settings.configuration.beforeRunTasks
    return settings
  }

  protected open fun getRunConfigurationName(): String =
    BazelRunnerActionNaming.getRunActionName(
      isDebugAction = isDebugAction,
      isRunConfigName = true,
      includeTargetNameInText = includeTargetNameInText,
      project = targetElement.project,
      target = target.id,
    )
}
