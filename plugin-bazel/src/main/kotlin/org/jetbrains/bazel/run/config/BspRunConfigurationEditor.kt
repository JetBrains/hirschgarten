package org.jetbrains.bazel.run.config

import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.ui.BeforeRunFragment
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.CommonTags
import com.intellij.execution.ui.RunConfigurationFragmentedEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.addBeforeRunFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import org.jetbrains.bazel.run.BspRunConfigurationState

/**
 * The base editor for a BSP run configuration.
 * Takes care of targets, the common settings and sets up the handler-specific settings editor.
 */
class BspRunConfigurationEditor(private val runConfiguration: BspRunConfiguration) :
  RunConfigurationFragmentedEditor<BspRunConfiguration>(
    runConfiguration,
    RunConfigurationExtensionManager.getInstance(),
  ) {
  override fun createRunFragments(): List<SettingsEditorFragment<BspRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      addBeforeRunFragment(CompileStepBeforeRun.ID)
      addAll(BeforeRunFragment.createGroup())
      add(CommonTags.parallelRun())
      addBspTargetFragment()
      addStateEditorFragment()
    }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addStateEditorFragment() {
    val handler = runConfiguration.handler ?: return
    this.add(CommonParameterFragments.createHeader(handler.name))
    val stateEditor: SettingsEditor<BspRunConfigurationState<*>> =
      handler.state.getEditor(
        runConfiguration,
      ) as SettingsEditor<BspRunConfigurationState<*>>
    Disposer.register(this@BspRunConfigurationEditor, stateEditor)

    this.addSettingsEditorFragment(
      object : SettingsFragmentInfo {
        override val settingsId: String = "bsp.state.editor"
        override val settingsName: String = "Handler settings"
        override val settingsGroup: String = "BSP"
        override val settingsPriority: Int = 1
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
        override val settingsHint: String? = null
        override val settingsActionHint: String? = null
      },
      { stateEditor.component },
      { _, _ ->
        stateEditor.resetFrom(handler.state)
      },
      { _, _ ->
        stateEditor.applyTo(handler.state)
      },
    )
  }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addBspTargetFragment() {
    this.addLabeledSettingsEditorFragment(
      object : LabeledSettingsFragmentInfo { // TODO: Use bundle
        override val editorLabel: String = "Build target"
        override val settingsId: String = "bsp.target.fragment"
        override val settingsName: String = "Build target"
        override val settingsGroup: String = "BSP"
        override val settingsHint: String = "Build target"
        override val settingsActionHint: String = "Build target"
      },
      { JBTextField().apply { isEditable = false } },
      { s, c ->
        c.text = s.targets.joinToString(", ") { it.uri }
      },
      { _, _ ->
        {}
      },
      { true },
    )
  }
}
