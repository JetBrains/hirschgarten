package org.jetbrains.bazel.runnerAction

import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import org.jetbrains.bazel.run.synthetic.GENERATE_SYNTHETIC_PROVIDER_ID
import org.jetbrains.bazel.run.synthetic.GenerateSyntheticTargetRunTaskProvider
import org.jetbrains.bazel.run.synthetic.SyntheticRunTargetTemplateGenerator
import org.jetbrains.bazel.run.synthetic.SyntheticRunTaskMarker
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bazel.ui.gutters.BazelRunLocation
import org.jetbrains.bsp.protocol.id

internal class SyntheticRunConfigurationProducer : LazyRunConfigurationProducer<BazelRunConfiguration>() {
  override fun isDumbAware(): Boolean = true

  override fun getConfigurationFactory(): ConfigurationFactory = bazelRunConfigurationFactory

  override fun setupConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>,
  ): Boolean {
    if (!BazelFeatureFlags.syntheticRunEnable) return false
    val location = context.location as? BazelRunLocation ?: return false
    val target = location.target
    // Binary/test targets already have "normal" run configurations produced from them
    if (target.kind.ruleType != RuleType.LIBRARY) return false

    val targetElement = location.originalLocation.psiElement
    if (targetElement is PsiFileSystemItem) return false
    val templateGenerator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, targetElement.language) ?: return false
    val syntheticTargetId = templateGenerator.getSyntheticTargetLabel(target, targetElement)

    // this runner is inferred from the original target
    val originalTargetProvider = RunHandlerProvider.getRunHandlerProvider(listOf(target.kind)) ?: return false
    configuration.updateRunProvider(listOf(syntheticTargetId), originalTargetProvider)

    val provider =
      BeforeRunTaskProvider.getProvider(configuration.project, GENERATE_SYNTHETIC_PROVIDER_ID) as GenerateSyntheticTargetRunTaskProvider
    val params = templateGenerator.getSyntheticParams(target, targetElement)
    val task = provider.createTask(configuration)
    val taskState = task.taskState
    taskState.target = target.id.toString()
    taskState.language = targetElement.language.id
    taskState.params = params.data
    configuration.setBeforeRunTasksFromHandler(listOf(task) + configuration.beforeRunTasks)

    configuration.name = BazelPluginBundle.message("runconfig.synthetic", target.id.toShortString(configuration.project))

    return true
  }

  override fun isConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
  ): Boolean {
    val target = (context.location as? BazelRunLocation)?.target ?: return false
    if (target.kind.ruleType != RuleType.LIBRARY) return false
    val targetElement = context.psiLocation ?: return false
    if (targetElement is PsiFileSystemItem) return false
    val templateGenerator = SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, targetElement.language) ?: return false
    val syntheticTargetId = templateGenerator.getSyntheticTargetLabel(target, targetElement)

    if (configuration.targets != listOf(syntheticTargetId)) return false
    return configuration.beforeRunTasks.any { it is SyntheticRunTaskMarker }
  }

  override fun isPreferredConfiguration(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean =
    !other.isProducedBy(BazelRunConfigurationProducer::class.java)
}
