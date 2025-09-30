package org.jetbrains.bazel.run.config

import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties

// Use BazelRunConfigurationType.createTemplateConfiguration(project) to create a new BazelRunConfiguration.
class BazelRunConfiguration internal constructor(
  private val project: Project,
  name: String,
  configurationType: BazelRunConfigurationType,
) : LocatableConfigurationBase<RunProfileState>(project, configurationType, name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  SMRunnerConsolePropertiesProvider,
  HotswappableRunConfiguration {
  private val logger: Logger = logger<BazelRunConfiguration>()

  /** The BSP-specific parts of the last serialized state of this run configuration. */
  private var bspElementState = Element(BSP_STATE_TAG)

  var targets: List<Label> = emptyList()
    private set // private because we need to set the targets directly when running readExternal

  fun updateTargets(newTargets: List<Label>, runHandlerProvider: RunHandlerProvider? = null) {
    targets = newTargets
    updateHandlerIfDifferentProvider(runHandlerProvider ?: RunHandlerProvider.getRunHandlerProvider(project, newTargets))
  }

  private var handlerProvider: RunHandlerProvider? = null

  var handler: BazelRunHandler? = null
    private set

  private fun updateHandlerIfDifferentProvider(newProvider: RunHandlerProvider) {
    if (newProvider == handlerProvider) return
    updateHandler(newProvider)
  }

  private fun updateHandler(newProvider: RunHandlerProvider) {
    try {
      handler?.state?.writeExternal(bspElementState)
    } catch (e: WriteExternalException) {
      logger.error("Failed to write BSP state", e)
    }
    handlerProvider = newProvider
    handler = newProvider.createRunHandler(this)

    try {
      handler?.state?.readExternal(bspElementState)
    } catch (e: Exception) {
      logger.error("Failed to read BSP state", e)
    }
  }

  override fun clone(): RunConfiguration {
    val result = super.clone() as BazelRunConfiguration
    // Deep-clone the handler to allow changing handler.state without changing the original configuration
    handlerProvider?.let {
      result.updateHandler(it)
    }
    return result
  }

  override fun getConfigurationEditor(): BazelRunConfigurationEditor = BazelRunConfigurationEditor(this)

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
    handler?.getRunProfileState(executor, environment)

  override fun readExternal(element: Element) {
    super.readExternal(element)

    // FUCKING CLONE
    val bspElement = element.getChild(BSP_STATE_TAG)?.clone() ?: return

    val targets = mutableListOf<String>()
    for (targetElement in bspElement.getChildren(TARGET_TAG)) {
      targets.add(targetElement.text)
    }

    this.targets = targets.map { Label.parse(it) }

    // It should be possible to load the configuration before the project is synchronized,
    // so we can't access targets' data here. Instead, we have to use the stored provider ID.
    // TODO: is that true?
    val providerId = bspElement.getAttributeValue(HANDLER_PROVIDER_ATTR)
    if (providerId == null) {
      logger.warn("No handler provider ID found in run configuration")
      return
    }

    bspElementState = bspElement

    val provider = RunHandlerProvider.findRunHandlerProvider(providerId)
    if (provider != null) {
      updateHandlerIfDifferentProvider(provider)
      // TODO the above already reads

      val handlerStateElement = bspElement.getChild(HANDLER_STATE_TAG) ?: return

      try {
        handler?.state?.readExternal(handlerStateElement)
      } catch (e: Exception) {
        logger.error("Failed to read BSP state", e)
      }
    } else {
      logger.warn("Failed to find run handler provider with ID $providerId")
      val newProvider = RunHandlerProvider.getRunHandlerProvider(project, this.targets)
      updateHandlerIfDifferentProvider(newProvider)
    }
  }

  // TODO: ideally we'd use an existing serialization mechanism like https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
  //  but it's hard to figure out how to implement it in our case, so for now let's use the franken-implementation
  //  inspired by Google's plugin (which probably predates modern IJ state serialization)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)

    val provider = handlerProvider
    val handler = handler

    if (provider == null || handler == null) {
      return
    }

    bspElementState.removeChildren(TARGET_TAG)

    for (target in targets) {
      val targetElement = Element(TARGET_TAG)
      targetElement.text = target.toString()
      bspElementState.addContent(targetElement)
    }

    bspElementState.setAttribute(HANDLER_PROVIDER_ATTR, provider.id)

    val handlerState = Element(HANDLER_STATE_TAG)

    handler.state.writeExternal(handlerState)

    bspElementState.removeChildren(HANDLER_STATE_TAG)
    bspElementState.addContent(handlerState)

    element.addContent(bspElementState.clone())
  }

  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties =
    BazelTestConsoleProperties(this, executor)

  override fun getAffectedTargets(): List<Label> = targets

  companion object {
    private const val TARGET_TAG = "bsp-target"
    private const val BSP_STATE_TAG = "bsp-state"
    private const val HANDLER_STATE_TAG = "handler-state"
    private const val HANDLER_PROVIDER_ATTR = "handler-provider-id"

    // Used in BazelRerunFailedTestsAction
    public val BAZEL_RUN_CONFIGURATION_KEY = Key.create<BazelRunConfiguration>("BAZEL_RUN_CONFIGURATION_KEY")

    fun get(environment: ExecutionEnvironment): BazelRunConfiguration =
      environment.getUserData(BAZEL_RUN_CONFIGURATION_KEY) ?: environment.runProfile as BazelRunConfiguration
  }
}
