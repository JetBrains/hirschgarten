package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import org.jetbrains.bazel.run.state.HasEnv
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id

abstract class BazelRunConfigurationProducer : LazyRunConfigurationProducer<BazelRunConfiguration>() {
  override fun isDumbAware(): Boolean = true

  override fun getConfigurationFactory(): ConfigurationFactory = bazelRunConfigurationFactory

  @ApiStatus.Internal
  override fun setupConfigurationFromContext(
    configuration: BazelRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>,
  ): Boolean {
    val location = context.location as? BazelRunLocation ?: return false
    val target = location.target
    if (!acceptsTarget(target)) return false
    val element = location.originalLocation.psiElement
    val gutterAction = getGutterAction(element, target) ?: return false
    configuration.updateTargets(listOf(target.id), RunHandlerProvider.getRunHandlerProvider(listOf(target.kind)))
    configuration.handler?.state?.let { gutterAction.applyTo(it) } ?: return false
    configuration.handler?.extensionsManager?.extendCreatedConfiguration(configuration, location)
    val targetName = target.id.toShortString(context.project)
    configuration.name = gutterAction.additionalLocationString?.let {
      BazelPluginBundle.message(
        "runconfig.name.and.location",
        targetName,
        gutterAction.additionalLocationString,
      )
    } ?: targetName
    return true
  }

  @ApiStatus.Internal
  override fun isConfigurationFromContext(configuration: BazelRunConfiguration, context: ConfigurationContext): Boolean {
    val location = context.location as? BazelRunLocation ?: return false
    val target = location.target
    if (!acceptsTarget(target)) return false
    if (configuration.targets != listOf(target.id)) return false
    val element = location.originalLocation.psiElement
    val gutterAction = getGutterAction(element, target) ?: return false
    return configuration.handler?.state?.let { gutterAction.isFromContext(it) } ?: false
  }

  abstract fun getGutterAction(
    element: PsiElement,
    target: BuildTarget,
  ): GutterAction?

  protected fun acceptsTarget(target: BuildTarget): Boolean =
    target.kind.isExecutable

  class GutterAction(
    val testFilter: String? = null,
    val programArguments: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val additionalLocationString: String? = null,
  ) {
    @ApiStatus.Internal
    fun applyTo(state: BazelRunConfigurationState<*>) {
      (state as? HasTestFilter)?.testFilter = testFilter
      (state as? HasProgramArguments)?.programArguments = formatProgramArguments(programArguments)
      (state as? HasEnv)?.env?.envs?.putAll(env)
    }

    @ApiStatus.Internal
    fun isFromContext(state: BazelRunConfigurationState<*>): Boolean {
      (state as? HasTestFilter)?.let { if (testFilter != it.testFilter) return false }
      (state as? HasProgramArguments)?.let { if (formatProgramArguments(programArguments) != it.programArguments) return false }
      (state as? HasEnv)?.env?.let { if (it.envs + env != it.envs) return false }
      return true
    }

    private fun formatProgramArguments(arguments: List<String>): String? =
      arguments.joinToString(" ") { argument ->
        val escaped = argument.replace("\"", "\\\"")
        "\"$escaped\""
      }.takeIf { it.isNotEmpty() }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as GutterAction

      if (testFilter != other.testFilter) return false
      if (programArguments != other.programArguments) return false
      if (env != other.env) return false
      if (additionalLocationString != other.additionalLocationString) return false

      return true
    }

    override fun hashCode(): Int {
      var result = testFilter.hashCode()
      result = 31 * result + programArguments.hashCode()
      result = 31 * result + env.hashCode()
      result = 31 * result + additionalLocationString.hashCode()
      return result
    }

    @ApiStatus.Internal
    fun copy(
      testFilter: String? = this.testFilter,
      programArguments: List<String> = this.programArguments,
      env: Map<String, String> = this.env,
      additionalLocationString: String? = this.additionalLocationString,
    ): GutterAction = GutterAction(testFilter, programArguments, env, additionalLocationString)
  }
}
