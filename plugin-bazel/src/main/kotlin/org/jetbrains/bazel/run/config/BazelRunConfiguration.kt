package org.jetbrains.bazel.run.config

import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsActions
import org.jdom.Element
import org.jetbrains.bazel.config.BazelPluginBundle.message
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties
import org.jetbrains.bazel.target.targetUtils

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

  var targets: List<Label> = emptyList()
    private set // private because we need to set the targets directly when running readExternal

  var doVisibilityCheck: Boolean = true
    private set

  override fun checkConfiguration() {
    val utils = project.targetUtils
    val selectedTargets = targets.map {
      val target = utils.getBuildTargetForLabel(it)
        ?: throw RuntimeConfigurationError(message("runconfig.bazel.errors.target.not.found", it))
      if (!target.kind.isExecutable) throw RuntimeConfigurationError(message("runconfig.bazel.errors.target.not.executable", it))
      target
    }
    if (selectedTargets.isEmpty()) throw RuntimeConfigurationError(message("runconfig.bazel.errors.no.targets"))
    val providers = selectedTargets
      .mapTo(mutableSetOf()) { target ->
        RunHandlerProvider
          .getRunHandlerProvider(listOf(target))
          ?: throw RuntimeConfigurationError(message("runconfig.bazel.errors.target.not.supported", target.id))
      }
    if (providers.size > 1) {
      throw RuntimeConfigurationError(
        message("runconfig.bazel.errors.multiple.platform.targets", providers.joinToString(", ") { it.id }),
      )
    }
  }

  fun updateTargets(newTargets: List<Label>, runHandlerProvider: RunHandlerProvider? = null) {
    targets = newTargets
    updateHandlerIfDifferentProvider(runHandlerProvider ?: RunHandlerProvider.getRunHandlerProvider(project, newTargets))
  }

  fun updateRunProvider(newTargets: List<Label>, runHandlerProvider: RunHandlerProvider) {
    targets = newTargets
    updateHandler(runHandlerProvider)
  }

  fun disableVisibilityCheck() {
    doVisibilityCheck = false
  }

  private var handlerProvider: RunHandlerProvider? = null

  var handler: BazelRunHandler? = null
    private set

  private fun updateHandlerIfDifferentProvider(newProvider: RunHandlerProvider) {
    if (newProvider == handlerProvider) return
    updateHandler(newProvider)
  }

  private fun updateHandler(newProvider: RunHandlerProvider) {
    val bspBeforeChange = createBspElement()
    handlerProvider = newProvider
    handler = newProvider.createRunHandler(this)
    bspBeforeChange ?: return
    val oldHandlerState = bspBeforeChange.getChild(HANDLER_STATE_TAG) ?: return
    try {
      handler?.state?.readExternal(oldHandlerState)
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

    doVisibilityCheck = bspElement.getAttributeValue(CHECK_VISIBILITY_ATTR)?.toBoolean() ?: true
  }

  // TODO: ideally we'd use an existing serialization mechanism like https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
  //  but it's hard to figure out how to implement it in our case, so for now let's use the franken-implementation
  //  inspired by Google's plugin (which probably predates modern IJ state serialization)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    val bspState = createBspElement() ?: return
    element.removeChildren(BSP_STATE_TAG)
    element.addContent(bspState)
  }

  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties =
    BazelTestConsoleProperties(this, executor)

  override fun getAffectedTargets(): List<Label> = targets

  override fun suggestedName(): @NlsActions.ActionText String = targets.joinToString(" ")

  private fun createBspElement(): Element? {
    val provider = handlerProvider ?: return null
    val handler = handler ?: return null
    return Element(BSP_STATE_TAG).apply {
      for (target in targets) {
        val targetElement = Element(TARGET_TAG)
        targetElement.text = target.toString()
        addContent(targetElement)
      }
      setAttribute(HANDLER_PROVIDER_ATTR, provider.id)
      val handlerState = Element(HANDLER_STATE_TAG)
      handler.state.writeExternal(handlerState)
      addContent(handlerState)

      setAttribute(CHECK_VISIBILITY_ATTR, doVisibilityCheck.toString())
    }
  }

  companion object {
    private const val TARGET_TAG = "bsp-target"
    private const val BSP_STATE_TAG = "bsp-state"
    private const val HANDLER_STATE_TAG = "handler-state"
    private const val HANDLER_PROVIDER_ATTR = "handler-provider-id"
    private const val CHECK_VISIBILITY_ATTR = "check-visibility"

    // Used in BazelRerunFailedTestsAction
    public val BAZEL_RUN_CONFIGURATION_KEY = Key.create<BazelRunConfiguration>("BAZEL_RUN_CONFIGURATION_KEY")

    fun get(environment: ExecutionEnvironment): BazelRunConfiguration =
      environment.getUserData(BAZEL_RUN_CONFIGURATION_KEY) ?: environment.runProfile as BazelRunConfiguration
  }
}
