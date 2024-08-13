package org.jetbrains.plugins.bsp.android.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBLabel
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import javax.swing.JComponent

data object AndroidBspRunConfigurationState : BspRunConfigurationState<AndroidBspRunConfigurationState>() {
  override fun getEditor(configuration: BspRunConfiguration) =
    object : SettingsEditor<AndroidBspRunConfigurationState>() {
      override fun resetEditorFrom(state: AndroidBspRunConfigurationState) {}

      override fun applyEditorTo(state: AndroidBspRunConfigurationState) {}

      override fun createEditor(): JComponent = JBLabel("Android has no handler specific settings")
    }
}
