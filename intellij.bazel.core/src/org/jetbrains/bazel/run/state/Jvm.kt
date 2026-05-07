package org.jetbrains.bazel.run.state

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.ui.CommandLinePanel
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.TextComponentEmptyText
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.config.BazelRunConfiguration

@ApiStatus.Internal
interface HasJavaVmOptions {
  var javaVmOptions: String?
}

private val BazelRunConfiguration.javaVmOptionsState: HasJavaVmOptions?
  get() = handler?.state as? HasJavaVmOptions

@ApiStatus.Internal
fun vmOptions(): SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> {
  val group = ExecutionBundle.message("group.java.options")
  val vmOptions = RawCommandLineEditor()
  CommandLinePanel.setMinimumWidth(vmOptions, 400)
  CommonParameterFragments.setMonospaced(vmOptions.textField)
  val message = ExecutionBundle.message("run.configuration.java.vm.parameters.empty.text")
  vmOptions.editorField.accessibleContext.accessibleName = message
  vmOptions.editorField.emptyText.text = message
  MacrosDialog.addMacroSupport(vmOptions.editorField, MacrosDialog.Filters.ALL) { false }
  TextComponentEmptyText.setupPlaceholderVisibility(vmOptions.editorField)
  val vmParameters: SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> =
    SettingsEditorFragment(
      "vmParameters",
      ExecutionBundle.message("run.configuration.java.vm.parameters.name"),
      group,
      vmOptions,
      15,
      { configuration, component ->
        component.text = configuration.javaVmOptionsState?.javaVmOptions
      },
      { configuration, component ->
        configuration.javaVmOptionsState?.javaVmOptions = if (component.isVisible) component.text else null
      },
      { configuration -> StringUtil.isNotEmpty(configuration.javaVmOptionsState?.javaVmOptions) },
    )
  vmParameters.setHint(ExecutionBundle.message("run.configuration.java.vm.parameters.hint"))
  vmParameters.actionHint =
    ExecutionBundle.message("specify.vm.options.for.running.the.application")
  vmParameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }

  return vmParameters
}
