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
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import javax.swing.JCheckBox

@ApiStatus.Internal
interface HasEnv {
  var env: EnvironmentVariablesDataOptions
}

private val BazelRunConfiguration.envState: HasEnv?
  get() = handler?.state as? HasEnv

// Copied from org.jetbrains.plugins.terminal.EnvironmentVariablesDataOptions
// We can't use it directly because it comes from a different plugin
@Tag("")
@ApiStatus.Internal
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

@ApiStatus.Internal
fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addEnvironmentFragment() =
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
    { envState?.env?.envs.orEmpty() },
    {
      val env = envState?.env ?: return@addEnvironmentFragment
      env.set(env.get().with(it))
    },
    { envState?.env?.isPassParentEnvs ?: false },
    { envState?.env?.isPassParentEnvs = it },
    hideWhenEmpty = false,
  )

@ApiStatus.Internal
interface HasProgramArguments {
  var programArguments: String?
}

private val BazelRunConfiguration.programArgumentsState: HasProgramArguments?
  get() = handler?.state as? HasProgramArguments

@ApiStatus.Internal
fun programArgumentsFragment(): SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> {
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
  val parameters: SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> =
    SettingsEditorFragment(
      "commandLineParameters",
      ExecutionBundle.message("run.configuration.program.parameters.name"),
      null,
      programArguments,
      100,
      { configuration, component ->
        component.text = configuration.programArgumentsState?.programArguments
      },
      { configuration, component ->
        configuration.programArgumentsState?.programArguments = component.text
      },
      { true },
    )
  parameters.isRemovable = false
  parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
  parameters.setHint(ExecutionBundle.message("run.configuration.program.parameters.hint"))

  return parameters
}

@ApiStatus.Internal
interface HasBazelParams {
  var additionalBazelParams: String?
}

private val BazelRunConfiguration.bazelParamsState: HasBazelParams?
  get() = handler?.state as? HasBazelParams

@ApiStatus.Internal
fun bazelParamsFragment(): SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> {
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
  val parameters: SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> =
    SettingsEditorFragment(
      "bazelParameters",
      BazelPluginBundle.message("runconfig.bazel.params"),
      null,
      bazelParams,
      100,
      { configuration, component ->
        component.text = configuration.bazelParamsState?.additionalBazelParams ?: ""
      },
      { configuration, component ->
        configuration.bazelParamsState?.additionalBazelParams = component.text
      },
      { true },
    )
  parameters.isRemovable = false
  parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
  parameters.setHint(BazelPluginBundle.message("runconfig.bazel.params"))

  return parameters
}


@ApiStatus.Internal
interface HasRunWithBazel {
  var runWithBazel: Boolean
}

private val BazelRunConfiguration.runWithBazelState: HasRunWithBazel?
  get() = handler?.state as? HasRunWithBazel

@ApiStatus.Internal
fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addRunWithBazelFragment(): SettingsEditorFragment<BazelRunConfiguration, DialogPanel> {
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
          cell(checkBox).contextHelp(BazelPluginBundle.message("runconfig.run.with.bazel.hint"))
        }
      }
    },
    { configuration, _ -> checkBox.isSelected = configuration.runWithBazelState?.runWithBazel ?: false },
    { configuration, _ -> configuration.runWithBazelState?.runWithBazel = checkBox.isSelected },
  )
}
