package org.jetbrains.plugins.bsp.intellij

import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.options.SettingsEditor
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.HasIntellijSdkName
import org.jetbrains.plugins.bsp.run.state.HasJavaVmOptions
import org.jetbrains.plugins.bsp.run.state.HasProgramArguments
import org.jetbrains.plugins.bsp.run.state.intellijSdkFragment
import org.jetbrains.plugins.bsp.run.state.programArgumentsFragment
import org.jetbrains.plugins.bsp.run.state.vmOptions

class IntellijPluginRunHandlerState :
  BspRunConfigurationState<IntellijPluginRunHandlerState>(),
  HasJavaVmOptions,
  HasProgramArguments,
  HasIntellijSdkName {
  @com.intellij.configurationStore.Property(description = "Java VM options")
  override var javaVmOptions: String? by string()

  @com.intellij.configurationStore.Property(description = "Program arguments")
  override var programArguments: String? by string()

  @com.intellij.configurationStore.Property(description = "IntelliJ SDK name")
  override var intellijSdkName: String? by string()

  override fun getEditor(configuration: BspRunConfiguration): SettingsEditor<IntellijPluginRunHandlerState> =
    IntellijPluginRunHandlerStateEditor(configuration)
}

class IntellijPluginRunHandlerStateEditor(private val config: BspRunConfiguration) :
  FragmentedSettingsEditor<IntellijPluginRunHandlerState>(config.handler?.state as IntellijPluginRunHandlerState) {
  override fun createFragments(): Collection<SettingsEditorFragment<IntellijPluginRunHandlerState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(programArgumentsFragment())
      add(vmOptions())
      add(intellijSdkFragment())
    }
}
