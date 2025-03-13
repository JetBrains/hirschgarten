package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.options.SettingsEditor
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.config.BazelRunConfiguration

class GenericRunState :
  BazelRunConfigurationState<GenericRunState>(),
  HasEnv,
  HasProgramArguments,
  HasWorkingDirectory,
  HasBazelParams {
  @com.intellij.configurationStore.Property(description = "Arguments")
  @get:Attribute("programArguments")
  override var programArguments: String? by string()

  @com.intellij.configurationStore.Property(description = "Working directory")
  @get:Attribute("workingDirectory")
  override var workingDirectory: String? by string()

  @com.intellij.configurationStore.Property(description = "Bazel parameters")
  override var additionalBazelParams: String? by string()

  // TODO: handle passing system environment variables
  @com.intellij.configurationStore.Property(description = "Environment variables")
  override var env: EnvironmentVariablesDataOptions by property(EnvironmentVariablesDataOptions())

  override fun getEditor(configuration: BazelRunConfiguration): SettingsEditor<GenericRunState> = GenericRunStateEditor(configuration)
}

class GenericRunStateEditor(private val config: BazelRunConfiguration) :
  FragmentedSettingsEditor<GenericRunState>(config.handler?.state as GenericRunState) {
  override fun createFragments(): Collection<SettingsEditorFragment<GenericRunState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader("Run Configuration"))

      add(bazelParamsFragment())
      add(programArgumentsFragment())
      add(workingDirectoryFragment(config))
      addEnvironmentFragment()
    }
}
