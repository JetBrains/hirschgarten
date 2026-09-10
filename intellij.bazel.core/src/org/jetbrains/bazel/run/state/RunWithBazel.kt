package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import javax.swing.JCheckBox

@ApiStatus.Internal
interface HasRunWithBazel {
  var runWithBazel: Boolean
}

@ApiStatus.Internal
class RunWithBazelFragment(
  val checkBox: JCheckBox = JCheckBox(BazelPluginBundle.message("runconfig.run.with.bazel")),
) : SettingsEditorFragment<BazelRunConfiguration, DialogPanel>(
    "Run with Bazel",
    BazelPluginBundle.message("runconfig.run.with.bazel"),
    BazelPluginBundle.message("runconfig.run.with.bazel"),
    panel {
      row {
        cell(checkBox).contextHelp(BazelPluginBundle.message("runconfig.run.with.bazel.hint"))
      }
    },
    /* priority = */ 1,
    SettingsEditorFragmentType.EDITOR,
    { configuration, _ -> checkBox.isSelected = configuration.runWithBazelState?.runWithBazel ?: false },
    { configuration, _ -> configuration.runWithBazelState?.runWithBazel = checkBox.isSelected },
    { true },
  ) {
  init {
    isRemovable = false
  }
}

private val BazelRunConfiguration.runWithBazelState: HasRunWithBazel?
  get() = handler?.state as? HasRunWithBazel
