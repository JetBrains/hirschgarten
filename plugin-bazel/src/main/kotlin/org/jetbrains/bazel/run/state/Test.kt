package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.bazel.config.BazelPluginBundle
import javax.swing.JTextField

interface HasTestFilter {
  var testFilter: String?
}

fun <C : HasTestFilter> SettingsEditorFragmentContainer<C>.addTestFilterFragment() =
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
    { state, component -> component.text = state.testFilter ?: "" },
    { state, component -> state.testFilter = component.text },
  )
