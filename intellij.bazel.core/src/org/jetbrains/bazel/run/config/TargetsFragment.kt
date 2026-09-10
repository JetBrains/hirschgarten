package org.jetbrains.bazel.run.config

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorLabeledComponent
import com.intellij.openapi.project.Project
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
    { configuration, component -> component.component.text = configuration.targets.joinToString(" ") { it.toString() } },
    { configuration, component -> configuration.updateTargets(parseTargets(component.component.text)) },
    { true },
  ) {
  constructor(project: Project) : this(createTargetsField(project))

  init {
    setHint(BazelPluginBundle.message("runconfig.targets.hint"))
    actionHint = BazelPluginBundle.message("runconfig.targets.action.hint")
  }

  val targets: List<Label>
    get() = targetsField.text.split(" ").mapNotNull { Label.parseOrNull(it.trim().ifEmpty { null }) }

  @Suppress("SplitModeApiUsage")
  fun addTargetsListener(listener: () -> Unit) {
    targetsField.addDocumentListener(
      object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) = listener()
      },
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

private fun parseTargets(text: String): List<Label> =
  if (text.isNotBlank()) {
    text
      .trim()
      .split(" ")
      .map(Label::parse)
  }
  else {
    emptyList()
  }
