package org.jetbrains.bazel.run.config

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorLabeledComponent
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.RunHandlerProvider
import javax.swing.DefaultComboBoxModel

@ApiStatus.Internal
class RunHandlerFragment(
  private val handlerCombo: ComboBox<RunHandlerProvider?>,
) : SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<ComboBox<RunHandlerProvider?>>>(
    "bazel.run.handler.fragment",
    BazelPluginBundle.message("runconfig.handler.name"),
    BazelPluginBundle.message("runconfig.targets.group"),
    SettingsEditorLabeledComponent(BazelPluginBundle.message("runconfig.handler.name"), handlerCombo),
    /* priority = */ 0,
    SettingsEditorFragmentType.EDITOR,
    { configuration, component -> resetHandlerCombo(component.component, configuration) },
    { configuration, component ->
      val selected = (component.component.selectedItem as? RunHandlerProvider)
        ?.takeIf { it in findOfferedProviders(configuration) }
      configuration.updateForcedHandlerProvider(selected?.id)
    },
    { true },
  ) {
  constructor() : this(createHandlerCombo())

  init {
    setHint(BazelPluginBundle.message("runconfig.handler.hint"))
    actionHint = BazelPluginBundle.message("runconfig.handler.action.hint")
    handlerCombo.addActionListener { fireEditorStateChanged() }
  }
}

private fun findSelectedProvider(configuration: BazelRunConfiguration): RunHandlerProvider? =
  configuration.forcedHandlerProviderId?.let { RunHandlerProvider.findRunHandlerProvider(it) }

private fun createHandlerCombo(): ComboBox<RunHandlerProvider?> {
  val combo = ComboBox<RunHandlerProvider?>()
  combo.renderer = textListCellRenderer(BazelPluginBundle.message("runconfig.handler.auto")) { it.providedRunHandlerName }
  return combo
}

private fun resetHandlerCombo(combo: ComboBox<RunHandlerProvider?>, configuration: BazelRunConfiguration) {
  val offered = findOfferedProviders(configuration)
  val selected = findSelectedProvider(configuration)?.takeIf { it in offered }
  val items = listOf(null) + offered
  combo.model = DefaultComboBoxModel(items.toTypedArray())
  combo.selectedItem = selected
  combo.isEnabled = items.size > 1 // disable when there's nothing to choose
}

private fun findOfferedProviders(configuration: BazelRunConfiguration): List<RunHandlerProvider> {
  val defaultProvider =
    configuration.targets
      .takeIf { it.isNotEmpty() }
      ?.let { RunHandlerProvider.getRunHandlerProviderOrNull(configuration.project, it) }
  val isTestConfig = defaultProvider?.isTestHandler
  return RunHandlerProvider.findSelectableRunHandlerProviders().filter {
    // offer only test handlers for test targets and vice versa
    isTestConfig == null || it.isTestHandler == isTestConfig
  }
}
