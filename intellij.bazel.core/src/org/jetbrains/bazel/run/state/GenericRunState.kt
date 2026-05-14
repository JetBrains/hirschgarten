package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.config.BazelRunConfiguration

@ApiStatus.Internal
class GenericRunState : AbstractGenericRunState<GenericRunState>()

@ApiStatus.Internal
open class AbstractGenericRunState<T : AbstractGenericRunState<T>> :
  BazelRunConfigurationState<T>(),
  HasEnv,
  HasProgramArguments,
  HasBazelParams {
  @com.intellij.configurationStore.Property(description = "Arguments")
  @get:Attribute("programArguments")
  override var programArguments: String? by string()

  @com.intellij.configurationStore.Property(description = "Bazel parameters")
  override var additionalBazelParams: String? by string()

  // TODO: handle passing system environment variables
  @com.intellij.configurationStore.Property(description = "Environment variables")
  override var env: EnvironmentVariablesDataOptions by property(EnvironmentVariablesDataOptions())

  override fun createFragments(configuration: BazelRunConfiguration): Collection<SettingsEditorFragment<BazelRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(bazelParamsFragment())
      add(programArgumentsFragment())
      addEnvironmentFragment()
    }
}
