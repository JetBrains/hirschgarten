package org.jetbrains.plugins.bsp.run.state

import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import javax.swing.JTextField

interface HasTestFilter {
  var testFilter: String?
}

fun <C : HasTestFilter> SettingsEditorFragmentContainer<C>.addTestFilterFragment() =
  addSettingsEditorFragment(
    object : SettingsFragmentInfo {
      override val settingsActionHint: String = "Test Filter Action Hint"
      override val settingsGroup: String = "Test Filter Group"
      override val settingsHint: String = "Test Filter Hint"
      override val settingsId: String = "Test Filter ID"
      override val settingsName: String = "Test Filter Name"
      override val settingsPriority: Int = 0
      override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
    },
    { JTextField() },
    { state, component -> component.text = state.testFilter ?: "" },
    { state, component -> state.testFilter = component.text },
  )
