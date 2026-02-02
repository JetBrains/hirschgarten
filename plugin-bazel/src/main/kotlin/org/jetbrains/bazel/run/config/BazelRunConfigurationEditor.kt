package org.jetbrains.bazel.run.config

import com.intellij.execution.ui.BeforeRunComponent
import com.intellij.execution.ui.BeforeRunFragment
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.CommonTags
import com.intellij.execution.ui.RunConfigurationFragmentedEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.Disposer
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.util.textCompletion.TextFieldWithCompletion
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.target.targetUtils

/**
 * The base editor for a Bazel run configuration.
 * Takes care of targets, the common settings and sets up the handler-specific settings editor.
 */
class BazelRunConfigurationEditor(private val runConfiguration: BazelRunConfiguration) :
  RunConfigurationFragmentedEditor<BazelRunConfiguration>(
    runConfiguration,
    RunConfigurationExtensionManager.getInstance(),
  ) {
  override fun createRunFragments(): List<SettingsEditorFragment<BazelRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonTags.parallelRun())
      addBeforeRunFragment()
      addBspTargetFragment()
      addStateEditorFragment()
    }

  private fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addBeforeRunFragment() {
    val beforeRunComponent = BeforeRunComponent(this@BazelRunConfigurationEditor)
    add(BeforeRunFragment.createBeforeRun(beforeRunComponent, null))
    addAll(BeforeRunFragment.createGroup())
  }

  private fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addStateEditorFragment() {
    val handler = runConfiguration.handler ?: return
    this.add(CommonParameterFragments.createHeader(BazelPluginBundle.message("runconfig.header")))
    val stateEditor: SettingsEditor<BazelRunConfigurationState<*>> =
      handler.state.getEditor(
        runConfiguration,
      ) as SettingsEditor<BazelRunConfigurationState<*>>
    Disposer.register(this@BazelRunConfigurationEditor, stateEditor)

    this.addSettingsEditorFragment(
      settingsFragmentInfo = object : SettingsFragmentInfo {
        override val settingsId: String = "bsp.state.editor"
        override val settingsName: String = "Handler settings"
        override val settingsGroup: String = "BSP"
        override val settingsPriority: Int = 1
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
        override val settingsHint: String? = null
        override val settingsActionHint: String? = null
      },
      createComponent = { stateEditor.component },
      reset = { config, _ ->
        val state = config.handler?.state ?: return@addSettingsEditorFragment
        stateEditor.resetFrom(state)
      },
      apply = { config, _ ->
        val state = config.handler?.state ?: return@addSettingsEditorFragment
        stateEditor.applyTo(state)
      },
    )
  }

  private fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addBspTargetFragment() {
    this.addLabeledSettingsEditorFragment(
      object : LabeledSettingsFragmentInfo { // TODO: Use bundle
        override val settingsId: String = "bsp.target.fragment"
        override val editorLabel: String = "Targets to run"
        override val settingsName: String = "Targets to run"
        override val settingsGroup: String = "Bazel"
        override val settingsHint: String = "Specify all the targets to run separated by space. Each target must be executable!"
        override val settingsActionHint: String = "Specify the targets to run."
      },
      {
        val provider = TextFieldWithAutoCompletion.StringsCompletionProvider(
          /* variants = */
          project
            .targetUtils
            .allExecutableTargetLabels,
          /* icon = */ BazelPluginIcons.bazel,
        )
        TextFieldWithCompletion(
          /* project = */ project,
          /* provider = */ provider,
          /* value = */ "",
          /* oneLineMode = */ true,
          /* autoPopup = */ true,
          /* forceAutoPopup = */ false,
          /* showHint = */ true,
        )
      },
      { config, field ->
        field.text = config.targets.joinToString(" ") { it.toString() }
      },
      { config, field ->
        if (field.text.isNotBlank()) {
          val targets = field.text
            .trim()
            .split(" ")
            .map(Label::parse)
          config.updateTargets(targets)
        } else {
          config.updateTargets(emptyList())
        }
      },
      { true },
    )
  }
}
