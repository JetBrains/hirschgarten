package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.config.BazelRunConfiguration

@ApiStatus.Internal
class GenericTestState : AbstractGenericTestState<GenericTestState>()

@ApiStatus.Internal
open class AbstractGenericTestState<T : AbstractGenericTestState<T>> :
  BazelRunConfigurationState<T>(),
  HasEnv,
  HasProgramArguments,
  HasTestFilter,
  HasBazelParams {
  @com.intellij.configurationStore.Property(description = "Test filter")
  @get:Attribute("testFilter")
  override var testFilter: String? by string()

  @com.intellij.configurationStore.Property(description = "Arguments")
  @get:Attribute("programArguments")
  override var programArguments: String? by string()

  // TODO: this field will be duplicated in the xml, figure out what's causing it
  //  (probably related to the fact
  //   that writeExternal is called multiple times as somehow the change detection is not working)
  @com.intellij.configurationStore.Property(description = "Environment variables")
  override var env: EnvironmentVariablesDataOptions by property(EnvironmentVariablesDataOptions())

  @com.intellij.configurationStore.Property(description = "Bazel parameters")
  override var additionalBazelParams: String? by string()

  override fun createFragments(configuration: BazelRunConfiguration): Collection<SettingsEditorFragment<BazelRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(bazelParamsFragment())
      addTestFilterFragment()
      add(programArgumentsFragment())
      addEnvironmentFragment()
    }
}
