package org.jetbrains.bazel.run.state

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.ui.CommandLinePanel
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.TextComponentEmptyText

interface HasJavaVmOptions {
  var javaVmOptions: String?
}

fun <T : HasJavaVmOptions> vmOptions(): SettingsEditorFragment<T, RawCommandLineEditor> {
  val group = ExecutionBundle.message("group.java.options")
  val vmOptions = RawCommandLineEditor()
  CommandLinePanel.setMinimumWidth(vmOptions, 400)
  CommonParameterFragments.setMonospaced(vmOptions.textField)
  val message = ExecutionBundle.message("run.configuration.java.vm.parameters.empty.text")
  vmOptions.editorField.accessibleContext.accessibleName = message
  vmOptions.editorField.emptyText.text = message
  MacrosDialog.addMacroSupport(vmOptions.editorField, MacrosDialog.Filters.ALL) { false }
  TextComponentEmptyText.setupPlaceholderVisibility(vmOptions.editorField)
  val vmParameters: SettingsEditorFragment<T, RawCommandLineEditor> =
    SettingsEditorFragment(
      "vmParameters",
      ExecutionBundle.message("run.configuration.java.vm.parameters.name"),
      group,
      vmOptions,
      15,
      { configuration, c ->
        c.text = configuration.javaVmOptions
      },
      { configuration, c ->
        configuration.javaVmOptions = if (c.isVisible) c.text else null
      },
      { configuration -> StringUtil.isNotEmpty(configuration.javaVmOptions) },
    )
  vmParameters.setHint(ExecutionBundle.message("run.configuration.java.vm.parameters.hint"))
  vmParameters.actionHint =
    ExecutionBundle.message("specify.vm.options.for.running.the.application")
  vmParameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }

  return vmParameters
}
