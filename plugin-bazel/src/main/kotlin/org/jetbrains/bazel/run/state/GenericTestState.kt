package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.options.SettingsEditor
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.config.BazelRunConfiguration

class GenericTestState :
  BazelRunConfigurationState<GenericTestState>(),
  HasEnv,
  HasProgramArguments,
  HasWorkingDirectory,
  HasTestFilter,
  HasBazelParams {
  @com.intellij.configurationStore.Property(description = "Test filter")
  @get:Attribute("testFilter")
  override var testFilter: String? by string()

  @com.intellij.configurationStore.Property(description = "Arguments")
  @get:Attribute("programArguments")
  override var programArguments: String? by string()

  @com.intellij.configurationStore.Property(description = "Working directory")
  @get:Attribute("workingDirectory")
  override var workingDirectory: String? by string()

  // TODO: this field will be duplicated in the xml, figure out what's causing it
  //  (probably related to the fact
  //   that writeExternal is called multiple times as somehow the change detection is not working)
  @com.intellij.configurationStore.Property(description = "Environment variables")
  override var env: EnvironmentVariablesDataOptions by property(EnvironmentVariablesDataOptions())

  @com.intellij.configurationStore.Property(description = "Bazel parameters")
  override var additionalBazelParams: String? by string()

  override fun getEditor(configuration: BazelRunConfiguration): SettingsEditor<GenericTestState> = GenericTestStateEditor(configuration)
}

class GenericTestStateEditor(private val config: BazelRunConfiguration) :
  FragmentedSettingsEditor<GenericTestState>(config.handler?.state as GenericTestState) {
  override fun createFragments(): Collection<SettingsEditorFragment<GenericTestState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader("Test Configuration"))

      add(bazelParamsFragment())
      addTestFilterFragment()
      add(programArgumentsFragment())
      add(workingDirectoryFragment(config))
      addEnvironmentFragment()
    }
}
