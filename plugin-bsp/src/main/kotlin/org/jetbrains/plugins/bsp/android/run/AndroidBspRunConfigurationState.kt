package org.jetbrains.plugins.bsp.android.run

import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.HasUseMobileInstall
import org.jetbrains.plugins.bsp.run.state.useMobileInstallFragment

data object AndroidBspRunConfigurationState : BspRunConfigurationState<AndroidBspRunConfigurationState>(), HasUseMobileInstall {
  @com.intellij.configurationStore.Property(description = "Use mobile-install")
  override var useMobileInstall: Boolean by property(true)

  override fun getEditor(configuration: BspRunConfiguration) = AndroidBspRunConfigurationStateEditor(configuration)
}

class AndroidBspRunConfigurationStateEditor(config: BspRunConfiguration) :
  FragmentedSettingsEditor<AndroidBspRunConfigurationState>(config.handler?.state as AndroidBspRunConfigurationState) {
  override fun createFragments(): Collection<SettingsEditorFragment<AndroidBspRunConfigurationState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(useMobileInstallFragment())
    }
}
