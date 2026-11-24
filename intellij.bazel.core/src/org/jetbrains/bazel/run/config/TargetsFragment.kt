package org.jetbrains.bazel.run.config

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorLabeledComponent
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.util.textCompletion.TextFieldWithCompletion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetStorage

@ApiStatus.Internal
class TargetsFragment(
  private val targetsField: TextFieldWithCompletion,
) : SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<TextFieldWithCompletion>>(
  "bsp.target.fragment",
  BazelPluginBundle.message("runconfig.targets.name"),
  BazelPluginBundle.message("runconfig.targets.group"),
  SettingsEditorLabeledComponent(BazelPluginBundle.message("runconfig.targets.name"), targetsField),
  /* priority = */ 0,
  SettingsEditorFragmentType.EDITOR,
  ::resetEditorFromSettings,
  ::applyEditorToSettings,
  { true },
) {
  constructor(project: Project) : this(createTargetsField(project))

  init {
    setHint(BazelPluginBundle.message("runconfig.targets.hint"))
    actionHint = BazelPluginBundle.message("runconfig.targets.action.hint")
  }

  val targets: List<Label>
    get() =
      try {
        TargetListParser.parseString(targetsField.text).filter { it.toString().isNotEmpty() }
      } catch (_: IllegalArgumentException) {
        emptyList()
      }

  fun getTextFieldState(): TextFieldState? {
    val editor = targetsField.editor ?: return null
    return TextFieldState(
      editor.caretModel.offset,
      TextRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd),
    )
  }

  fun restoreTextFieldState(state: TextFieldState?) {
    if (state == null) return
    targetsField.setCaretPosition(state.caretOffset)
    targetsField.select(state.selection)
  }

  data class TextFieldState(val caretOffset: Int, val selection: TextRange)

  @Suppress("SplitModeApiUsage")
  fun addTargetsListener(listener: () -> Unit) {
    targetsField.addDocumentListener(
      object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) = listener()
      },
    )
  }
}

private fun resetEditorFromSettings(
  configuration: BazelRunConfiguration,
  component: SettingsEditorLabeledComponent<TextFieldWithCompletion>
) {
  component.component.text = configuration.targetListString
}

private fun applyEditorToSettings(
  configuration: BazelRunConfiguration,
  component: SettingsEditorLabeledComponent<TextFieldWithCompletion>
) {
  try {
    configuration.updateTargets(component.component.text)
  } catch (e: IllegalArgumentException) {
    throw ConfigurationException(
      BazelPluginBundle.message("runconfig.bazel.errors.invalid.targets"),
      e,
      ConfigurationException.getDefaultTitle(),
    )
  }
}

private fun createTargetsField(project: Project): TextFieldWithCompletion {
  val provider = TextFieldWithAutoCompletion.StringsCompletionProvider(
    /* variants = */
    project
      .targetStorage
      .allExecutableTargetLabels,
    /* icon = */ BazelPluginIcons.bazel,
  )
  return TextFieldWithCompletion(
    /* project = */ project,
    /* provider = */ provider,
    /* value = */ "",
    /* oneLineMode = */ true,
    /* autoPopup = */ true,
    /* forceAutoPopup = */ false,
    /* showHint = */ true,
  )
}
