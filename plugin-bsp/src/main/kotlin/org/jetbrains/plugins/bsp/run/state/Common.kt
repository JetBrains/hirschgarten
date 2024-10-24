package org.jetbrains.plugins.bsp.run.state

import com.intellij.configurationStore.Property
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.ui.CommandLinePanel
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.TextComponentEmptyText
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ThrowableRunnable
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap
import org.jetbrains.bsp.sdkcompat.ui.addBrowseFolderListenerCompat
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths

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

interface HasWorkingDirectory {
  var workingDirectory: String?
}

fun <T : HasWorkingDirectory> workingDirectoryFragment(
  configuration: RunConfiguration,
): SettingsEditorFragment<T, LabeledComponent<TextFieldWithBrowseButton>> {
  val textField = ExtendableTextField(10)
  MacrosDialog.addMacroSupport(
    textField,
    MacrosDialog.Filters.DIRECTORY_PATH,
  ) { false }
  val workingDirectoryField = TextFieldWithBrowseButton(textField)
  workingDirectoryField.addBrowseFolderListenerCompat(
    ExecutionBundle.message(
      "select.working.directory.message",
    ),
    null,
    configuration.project,
  )
  val field =
    LabeledComponent.create(
      workingDirectoryField,
      ExecutionBundle.message("run.configuration.working.directory.label"),
      "West",
    )
  val workingDirectorySettings: SettingsEditorFragment<T, LabeledComponent<TextFieldWithBrowseButton>> =
    SettingsEditorFragment(
      "workingDirectory",
      ExecutionBundle.message("run.configuration.working.directory.name"),
      null as String?,
      field,
      { settings, component ->
        (component.component as TextFieldWithBrowseButton).setText(settings.workingDirectory)
      },
      { settings, component ->
        settings.workingDirectory = component.component.text
      },
      { true },
    )
  workingDirectorySettings.isRemovable = false
  workingDirectorySettings.setValidation { settings ->
    val runnable =
      ThrowableRunnable<RuntimeConfigurationWarning> {
        val workingDir = settings.workingDirectory ?: return@ThrowableRunnable
        val exists =
          try {
            Files.exists(Paths.get(workingDir))
          } catch (e: InvalidPathException) {
            false
          }
        if (!exists) {
          throw RuntimeConfigurationWarning(
            ExecutionBundle.message(
              "dialog.message.working.directory.doesn.t.exist",
              workingDir,
            ),
          )
        }
      }
    val validationInfo =
      RuntimeConfigurationException.validate(
        textField,
        runnable,
      )
    listOf(validationInfo)
  }
  return workingDirectorySettings
}

interface HasProgramArguments {
  var programArguments: String?
}

fun <T : HasProgramArguments> programArgumentsFragment(): SettingsEditorFragment<T, RawCommandLineEditor> {
  val programArguments = RawCommandLineEditor()
  CommandLinePanel.setMinimumWidth(programArguments, 400)
  val message = ExecutionBundle.message("run.configuration.program.parameters.placeholder")
  programArguments.editorField.emptyText.setText(message)
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
      null as String?,
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
