package org.jetbrains.bazel.android.run

import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasUseMobileInstall
import org.jetbrains.bazel.run.state.useMobileInstallFragment

data object AndroidBazelRunConfigurationState : BazelRunConfigurationState<AndroidBazelRunConfigurationState>(), HasUseMobileInstall {
  @com.intellij.configurationStore.Property(description = "Use mobile-install")
  override var useMobileInstall: Boolean by property(true)

  override fun getEditor(configuration: BazelRunConfiguration) = AndroidBspRunConfigurationStateEditor(configuration)
}

class AndroidBspRunConfigurationStateEditor(config: BazelRunConfiguration) :
  FragmentedSettingsEditor<AndroidBazelRunConfigurationState>(config.handler?.state as AndroidBazelRunConfigurationState) {
  override fun createFragments(): Collection<SettingsEditorFragment<AndroidBazelRunConfigurationState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(useMobileInstallFragment())
    }
}
