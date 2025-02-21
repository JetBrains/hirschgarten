package org.jetbrains.bazel.languages.starlark.formatting.configuration

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.emptyText
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.text.nullize
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.formatting.BuildifierUtil
import java.io.File

class BuildifierConfigurable(val project: Project) : BoundSearchableConfigurable(BazelPluginBundle.message(DISPLAY_NAME_KEY), ID) {
  private var detectedBuildifierExecutable: File? = null
  private var storedState = BuildifierConfiguration.getBuildifierConfiguration(project)

  private lateinit var pathToBinaryRow: Row

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
    }

  init {
    detectedBuildifierExecutable = BuildifierUtil.detectBuildifierExecutable()
  }

  private fun updateUiState() {
    pathToBinaryRow.visible(true)
  }

  override fun isModified(): Boolean = storedState != applyToConfig(storedState.copy())

  private fun initForm() {
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

  companion object {
    const val ID = "bazel.buildifier.settings"
    const val DISPLAY_NAME_KEY = "buildifier.configurable.display.name"
  }
}
