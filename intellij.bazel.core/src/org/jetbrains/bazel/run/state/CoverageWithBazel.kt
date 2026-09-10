package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.NestedGroupFragment
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorLabeledComponent
import com.intellij.ui.components.JBTextField
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration

@ApiStatus.Internal
interface HasCoverageInstrumentationFilter {
  var coverageInstrumentationFilter: String?
}

@ApiStatus.Internal
class CoverageWithBazelFragment : NestedGroupFragment<BazelRunConfiguration>(
    "bazelCoverage",
    BazelPluginBundle.message("runconfig.coverage.name"),
    BazelPluginBundle.message("runconfig.coverage.group"),
    { false },
  ) {
  private var isAvailableWhen: () -> Boolean = { true }

  override fun createChildren(): List<SettingsEditorFragment<BazelRunConfiguration, *>> =
    listOf(coverageInstrumentationFilterFragment())

  override fun isAvailable(): Boolean = isAvailableWhen() && super.isAvailable()

  fun availableWhen(condition: () -> Boolean) {
    this.isAvailableWhen = condition
  }
}

private fun coverageInstrumentationFilterFragment():
  SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JBTextField>> {
  val filterField = JBTextField()
  CommonParameterFragments.setMonospaced(filterField)
  val message = BazelPluginBundle.message("runconfig.coverage.instrumentation.filter")
  filterField.accessibleContext.accessibleName = message
  filterField.emptyText.text = BazelPluginBundle.message("runconfig.coverage.instrumentation.filter.empty.text")
  val labeled = SettingsEditorLabeledComponent(message, filterField)
  val fragment =
    SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JBTextField>>(
      "bazelCoverageInstrumentationFilter",
      message,
      BazelPluginBundle.message("runconfig.coverage.group"),
      labeled,
      { configuration, component ->
        component.component.text = configuration.coverageInstrumentationFilterState?.coverageInstrumentationFilter.orEmpty()
      },
      { configuration, component ->
        configuration
          .coverageInstrumentationFilterState
          ?.coverageInstrumentationFilter = component.component.text.takeIf { it.isNotBlank() }
      },
      { configuration -> !configuration.coverageInstrumentationFilterState?.coverageInstrumentationFilter.isNullOrBlank() },
    )
  fragment.setHint(BazelPluginBundle.message("runconfig.coverage.instrumentation.filter.hint"))
  return fragment
}

private val BazelRunConfiguration.coverageInstrumentationFilterState: HasCoverageInstrumentationFilter?
  get() = handler?.state as? HasCoverageInstrumentationFilter
