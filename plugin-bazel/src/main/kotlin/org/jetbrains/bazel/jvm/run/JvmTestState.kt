package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.PortField
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.state.addEnvironmentFragment
import org.jetbrains.bazel.run.state.addTestFilterFragment
import org.jetbrains.bazel.run.state.bazelParamsFragment
import org.jetbrains.bazel.run.state.programArgumentsFragment

class JvmTestState :
  AbstractGenericTestState<JvmTestState>(),
  HasDebugPort {
  @com.intellij.configurationStore.Property(description = "Debug port")
  @get:Attribute("debugPort")
  override var debugPort: Int by property(5005)

  override fun getEditor(configuration: BazelRunConfiguration): SettingsEditor<JvmTestState> = JvmTestStateEditor(configuration)
}

class JvmTestStateEditor(private val config: BazelRunConfiguration) :
  FragmentedSettingsEditor<JvmTestState>(config.handler?.state as JvmTestState) {
  override fun createFragments(): Collection<SettingsEditorFragment<JvmTestState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader(BazelPluginBundle.message("jvm.runner.test.header")))
      addDebugPortFragment()
      add(bazelParamsFragment())
      addTestFilterFragment()
      add(programArgumentsFragment())
      addEnvironmentFragment()
    }
}

interface HasDebugPort {
  var debugPort: Int
}

fun <C : HasDebugPort> SettingsEditorFragmentContainer<C>.addDebugPortFragment() =
  addLabeledSettingsEditorFragment(
    object : LabeledSettingsFragmentInfo {
      override val settingsActionHint: String? = null
      override val settingsGroup: String = "bazel.fragment.debugPort"
      override val settingsHint: String? = null
      override val settingsId: String = "Debug Port ID"
      override val settingsName: String = "Debug Port"
      override val editorLabel: @NlsContexts.Label String = BazelPluginBundle.message("debug.editor.label")
      override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
    },
    {
      PortField()
    },
    { state, component -> component.number = state.debugPort },
    { state, component -> state.debugPort = component.number },
  )
