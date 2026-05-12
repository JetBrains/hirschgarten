package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.PortField
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.languages.projectview.runConfigRunWithBazel
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.state.HasRunWithBazel
import org.jetbrains.bazel.run.state.addEnvironmentFragment
import org.jetbrains.bazel.run.state.addRunWithBazelFragment
import org.jetbrains.bazel.run.state.addTestFilterFragment
import org.jetbrains.bazel.run.state.bazelParamsFragment
import org.jetbrains.bazel.run.state.programArgumentsFragment

@ApiStatus.Internal
class JvmTestState(project: Project) :
  AbstractGenericTestState<JvmTestState>(),
  HasDebugPort,
  HasRunWithBazel {
  @com.intellij.configurationStore.Property(description = "Debug port")
  @get:Attribute("debugPort")
  override var debugPort: Int by property(5005)

  @com.intellij.configurationStore.Property(description = "Run with Bazel")
  @get:Attribute("runWithBazel")
  override var runWithBazel: Boolean by property(project.projectView().runConfigRunWithBazel)

  override fun createFragments(configuration: BazelRunConfiguration): Collection<SettingsEditorFragment<BazelRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      addDebugPortFragment()
      add(bazelParamsFragment())
      addTestFilterFragment()
      add(programArgumentsFragment())
      addEnvironmentFragment()
      addRunWithBazelFragment()
    }
}

internal interface HasDebugPort {
  var debugPort: Int
}

private val BazelRunConfiguration.debugPortState: HasDebugPort?
  get() = handler?.state as? HasDebugPort

internal fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addDebugPortFragment() =
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
    { configuration, component -> component.number = configuration.debugPortState?.debugPort ?: 0 },
    { configuration, component -> configuration.debugPortState?.debugPort = component.number },
  )
