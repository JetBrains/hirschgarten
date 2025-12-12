package org.jetbrains.bazel.run.state

import com.intellij.configurationStore.Property
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.ui.CommandLinePanel
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.TextComponentEmptyText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap
import org.jetbrains.bazel.config.BazelPluginBundle
import javax.swing.JCheckBox

interface HasEnv {
  var env: EnvironmentVariablesDataOptions
}

// Copied from org.jetbrains.plugins.terminal.EnvironmentVariablesDataOptions
// We can't use it directly because it comes from a different plugin
@Tag("")
class EnvironmentVariablesDataOptions : BaseState() {
  // user order of env must be preserved - do not sort user input
  @Property(description = "Environment variables")
  @get:XMap(entryTagName = "env", keyAttributeName = "key")
  val envs by linkedMap<String, String>()

  var isPassParentEnvs by property(true)

  fun set(envData: EnvironmentVariablesData) {
    envs.clear()
    envs.putAll(envData.envs)
    isPassParentEnvs = envData.isPassParentEnvs
    incrementModificationCount()
  }

  fun get(): EnvironmentVariablesData = EnvironmentVariablesData.create(envs, isPassParentEnvs)
}

fun <C : HasEnv> SettingsEditorFragmentContainer<C>.addEnvironmentFragment() =
  addEnvironmentFragment(
    object : LabeledSettingsFragmentInfo {
      override val editorLabel: String = ExecutionBundle.message("environment.variables.component.title")
      override val settingsId: String = "external.system.environment.variables.fragment"
      override val settingsName: String = ExecutionBundle.message("environment.variables.fragment.name")
      override val settingsGroup: String = ExecutionBundle.message("group.operating.system")
      override val settingsHint: String = ExecutionBundle.message("environment.variables.fragment.hint")
      override val settingsActionHint: String =
        ExecutionBundle.message("set.custom.environment.variables.for.the.process")
    },
    { env.envs },
    { env.set(env.get().with(it)) },
    { env.isPassParentEnvs },
    { env.isPassParentEnvs = it },
    hideWhenEmpty = false,
  )

interface HasProgramArguments {
  var programArguments: String?
}

fun <T : HasProgramArguments> programArgumentsFragment(): SettingsEditorFragment<T, RawCommandLineEditor> {
  val programArguments = RawCommandLineEditor()
  CommandLinePanel.setMinimumWidth(programArguments, 400)
  val message = ExecutionBundle.message("run.configuration.program.parameters.placeholder")
  programArguments.editorField.emptyText.text = message
  programArguments.editorField.accessibleContext.accessibleName = message
  TextComponentEmptyText.setupPlaceholderVisibility(programArguments.editorField)
  CommonParameterFragments.setMonospaced(programArguments.textField)
  MacrosDialog.addMacroSupport(
    programArguments.editorField,
    MacrosDialog.Filters.ALL,
  ) { false }
  val parameters: SettingsEditorFragment<T, RawCommandLineEditor> =
    SettingsEditorFragment(
      "commandLineParameters",
      ExecutionBundle.message("run.configuration.program.parameters.name"),
      null,
      programArguments,
      100,
      { settings, component ->
        component.text = settings.programArguments
      },
      { settings, component ->
        settings.programArguments = component.text
      },
      { true },
    )
  parameters.isRemovable = false
  parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
  parameters.setHint(ExecutionBundle.message("run.configuration.program.parameters.hint"))

  return parameters
}

interface HasBazelParams {
  var additionalBazelParams: String?
}

fun <T : HasBazelParams> bazelParamsFragment(): SettingsEditorFragment<T, RawCommandLineEditor> {
  val bazelParams = RawCommandLineEditor()
  CommandLinePanel.setMinimumWidth(bazelParams, 400)
  val message = BazelPluginBundle.message("runconfig.bazel.params")
  bazelParams.editorField.emptyText.text = message
  bazelParams.editorField.accessibleContext.accessibleName = message
  TextComponentEmptyText.setupPlaceholderVisibility(bazelParams.editorField)
  CommonParameterFragments.setMonospaced(bazelParams.textField)
  MacrosDialog.addMacroSupport(
    bazelParams.editorField,
    MacrosDialog.Filters.ALL,
  ) { false }
  val parameters: SettingsEditorFragment<T, RawCommandLineEditor> =
    SettingsEditorFragment(
      "bazelParameters",
      BazelPluginBundle.message("runconfig.bazel.params"),
      null,
      bazelParams,
      100,
      { settings, component ->
        component.text = settings.additionalBazelParams
      },
      { settings, component ->
        settings.additionalBazelParams = component.text
      },
      { true },
    )
  parameters.isRemovable = false
  parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
  parameters.setHint(BazelPluginBundle.message("runconfig.bazel.params"))

  return parameters
}


interface HasRunWithBazel {
  var runWithBazel: Boolean
}

fun <C : HasRunWithBazel> SettingsEditorFragmentContainer<C>.addRunWithBazelFragment(): SettingsEditorFragment<C, DialogPanel> {
  val checkBox = JCheckBox(BazelPluginBundle.message("runconfig.run.with.bazel"))
  return addSettingsEditorFragment(
    object : SettingsFragmentInfo {
      override val settingsName: String = "Run with Bazel"
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
          cell(checkBox)
          contextHelp(BazelPluginBundle.message("runconfig.run.with.bazel.hint"))
        }
      }
    },
    { state, _ -> checkBox.isSelected = state.runWithBazel },
    { state, _ -> state.runWithBazel = checkBox.isSelected },
  )
}
