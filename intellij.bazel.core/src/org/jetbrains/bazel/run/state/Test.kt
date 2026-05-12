package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import javax.swing.JTextField

@ApiStatus.Internal
interface HasTestFilter {
  var testFilter: String?
}

private val BazelRunConfiguration.testFilterState: HasTestFilter?
  get() = handler?.state as? HasTestFilter

@ApiStatus.Internal
fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addTestFilterFragment() =
  addLabeledSettingsEditorFragment(
    object : LabeledSettingsFragmentInfo {
      override val settingsActionHint: String? = null
      override val settingsGroup: String = "bsp.fragment.testFilter"
      override val settingsHint: String? = null
      override val settingsId: String = "Test Filter ID"
      override val settingsName: String = "Test Filter"
      override val editorLabel: @NlsContexts.Label String = BazelPluginBundle.message("settings.editor.label.test.filter")
      override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
    },
    {
      JTextField().also {
        CommonParameterFragments.setMonospaced(it)
      }
    },
    { configuration, component -> component.text = configuration.testFilterState?.testFilter ?: "" },
    { configuration, component -> configuration.testFilterState?.testFilter = component.text },
  )
