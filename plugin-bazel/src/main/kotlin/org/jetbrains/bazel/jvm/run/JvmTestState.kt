package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.PortField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.state.addEnvironmentFragment
import org.jetbrains.bazel.run.state.addTestFilterFragment
import org.jetbrains.bazel.run.state.bazelParamsFragment
import org.jetbrains.bazel.run.state.programArgumentsFragment
import javax.swing.JCheckBox

class JvmTestState :
  AbstractGenericTestState<JvmTestState>(),
  HasDebugPort,
  HasTestWithBazel {
  @com.intellij.configurationStore.Property(description = "Debug port")
  @get:Attribute("debugPort")
  override var debugPort: Int by property(5005)

  @com.intellij.configurationStore.Property(description = "Test with Bazel")
  @get:Attribute("testWithBazel")
  override var testWithBazel: Boolean by property(BazelFeatureFlags.runConfigTestWithBazel)

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
      addTestWithBazelFragment()
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

interface HasTestWithBazel {
  var testWithBazel: Boolean
}

private fun <C : HasTestWithBazel> SettingsEditorFragmentContainer<C>.addTestWithBazelFragment(): SettingsEditorFragment<C, DialogPanel> {
  val checkBox = JCheckBox(BazelPluginBundle.message("runconfig.test.with.bazel"))
  return addSettingsEditorFragment(
    object : SettingsFragmentInfo {
      override val settingsName: String = "Test with Bazel"
      override val settingsId: String = settingsName
      override val settingsGroup = settingsName
      override val settingsPriority: Int = 1
      override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      override val settingsHint: String? = null
      override val settingsActionHint: String? = null
    },
    {
      panel {
        row {
          cell(checkBox).contextHelp(BazelPluginBundle.message("runconfig.test.with.bazel.hint"))
        }
      }
    },
    { state, _ -> checkBox.isSelected = state.testWithBazel },
    { state, _ -> state.testWithBazel = checkBox.isSelected },
  )
}
