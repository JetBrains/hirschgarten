package org.jetbrains.bazel.run.config

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelRunConfigurationEditorCustomizer {

  fun fragmentsCreated(configuration: BazelRunConfiguration, fragments: List<SettingsEditorFragment<BazelRunConfiguration, *>>)


  fun fragmentsReset(configuration: BazelRunConfiguration, fragments: List<SettingsEditorFragment<BazelRunConfiguration, *>>)

  companion object {
    internal val EP_NAME: ExtensionPointName<BazelRunConfigurationEditorCustomizer> =
      ExtensionPointName("org.jetbrains.bazel.runConfigurationEditorCustomizer")
  }
}
