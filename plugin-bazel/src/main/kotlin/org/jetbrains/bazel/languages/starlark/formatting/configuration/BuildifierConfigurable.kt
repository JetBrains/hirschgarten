package org.jetbrains.bazel.languages.starlark.formatting.configuration

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.emptyText
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.text.nullize
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.formatting.BuildifierUtil
import java.io.File
import javax.swing.JCheckBox

const val CONFIGURABLE_ID = "com.jetbrains.bazel.buildifier.configuration.BuildifierConfigurable"

class BuildifierConfigurable(val project: Project) : BoundConfigurable(BazelPluginBundle.message("buildifier.configurable.name")) {
  private var detectedBuildifierExecutable: File? = null
  private var storedState = BuildifierConfiguration.getBuildifierConfiguration(project)

  private lateinit var settingsPanel: Panel
  private lateinit var pathToBinaryRow: Row
  private lateinit var enableOnReformatCheckBox: JCheckBox

  private val buildifierExecutablePathField =
    TextFieldWithBrowseButton().apply {
      val title = BazelPluginBundle.message("buildifier.select.path.to.executable")
      val description = null
      addBrowseFolderListener(
        project,
        FileChooserDescriptorFactory
          .createSingleFileDescriptor()
          .withTitle(
            title,
          ).withDescription(description),
      )
    }

  var mainPanel: DialogPanel =
    panel {
      pathToBinaryRow =
        row(BazelPluginBundle.message("buildifier.executable.label")) {
          layout(RowLayout.LABEL_ALIGNED)
          cell(buildifierExecutablePathField)
            .validationInfo { buildifierExecutableValidationInfo() }
            .onChanged { updateUiState() }
            .align(AlignX.FILL)
          bottomGap(BottomGap.SMALL)
        }
      settingsPanel =
        panel {
          row(BazelPluginBundle.message("buildifier.use.section.label")) {
            layout(RowLayout.LABEL_ALIGNED)
            enableOnReformatCheckBox =
              checkBox(BazelPluginBundle.message("buildifier.enable.buildifier.checkbox.label")).component
            val shortcut = ActionManager.getInstance().getKeyboardShortcut(IdeActions.ACTION_EDITOR_REFORMAT)
            shortcut?.let { comment(KeymapUtil.getShortcutText(it)) }
          }
        }
    }

  init {
    detectedBuildifierExecutable = BuildifierUtil.detectBuildifierExecutable()
  }

  private fun canBeEnabled(): Boolean = storedState.pathToExecutable != null || buildifierExecutableValidationInfo() == null

  private fun updateUiState() {
    pathToBinaryRow.visible(true)

    val canBeEnabled = canBeEnabled()
    settingsPanel.enabled(canBeEnabled)
    enableOnReformatCheckBox.isSelected = storedState.enabledOnReformat && canBeEnabled
  }

  override fun isModified(): Boolean = storedState != applyToConfig(storedState.copy())

  private fun initForm() {
    enableOnReformatCheckBox.isSelected = storedState.enabledOnReformat

    buildifierExecutablePathField.emptyText.text = getBuildifierExecPathPlaceholderMessage()
    storedState.pathToExecutable?.let {
      buildifierExecutablePathField.text = it
    }
    updateUiState()
  }

  private fun getBuildifierExecPathPlaceholderMessage(): String =
    BuildifierUtil.detectBuildifierExecutable()?.let {
      BazelPluginBundle.message("buildifier.executable.auto.detected.path", it.absolutePath)
    } ?: BazelPluginBundle.message("buildifier.executable.not.found", if (SystemInfo.isWindows) 0 else 1)

  override fun reset() {
    initForm()
    updateUiState()
  }

  override fun apply() {
    applyToConfig(storedState)
  }

  override fun createPanel(): DialogPanel {
    mainPanel.registerValidators(disposable!!)
    return mainPanel
  }

  private fun applyToConfig(settings: BuildifierConfiguration): BuildifierConfiguration =
    settings.apply {
      enabledOnReformat = enableOnReformatCheckBox.isSelected

      pathToExecutable =
        if (buildifierExecutableValidationInfo() == null) {
          buildifierExecutablePathField.text.nullize()
            ?: BuildifierUtil.detectBuildifierExecutable()?.absolutePath
        } else {
          null
        }
    }

  private fun buildifierExecutableValidationInfo(): ValidationInfo? =
    BuildifierUtil.validateBuildifierExecutable(
      buildifierExecutablePathField.text.nullize() ?: BuildifierUtil.detectBuildifierExecutable()?.absolutePath,
    )
}
