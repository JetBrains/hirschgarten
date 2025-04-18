package org.jetbrains.bazel.ui.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.buildifier.BuildifierUtil
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sdkcompat.FileChooserDescriptorFactoryCompat
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.pathString

internal class BazelProjectSettingsConfigurable(private val project: Project) : SearchableConfigurable {
  private val projectViewPathField: TextFieldWithBrowseButton
  private val buildifierExecutablePathField: TextFieldWithBrowseButton
  private val runBuildifierOnSaveCheckBox: JBCheckBox
  private val showExcludedDirectoriesAsSeparateNodeCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    projectViewPathField = initProjectViewFileField()
    buildifierExecutablePathField = initBuildifierExecutablePathField()
    runBuildifierOnSaveCheckBox = initRunBuildifierOnSaveCheckBox()
    showExcludedDirectoriesAsSeparateNodeCheckBox = initShowExcludedDirectoriesAsSeparateNodeCheckBox()
  }

  private fun initProjectViewFileField(): TextFieldWithBrowseButton =
    TextFieldWithBrowseButton().apply {
      val title = "Select Path"
      val description = "Select the path for your project view file."
      addBrowseFolderListener(
        project,
        FileChooserDescriptorFactoryCompat
          .createSingleFileDescriptor()
          .withTitle(title)
          .withDescription(description),
      )
      whenTextChanged {
        if (text.isNotBlank()) {
          val newPath = Path(text)
          currentProjectSettings = currentProjectSettings.withNewProjectViewPath(newPath)
        }
      }
    }

  private fun initBuildifierExecutablePathField(): TextFieldWithBrowseButton =
    TextFieldWithBrowseButton().apply {
      val title = BazelPluginBundle.message("buildifier.select.path.to.executable")
      addBrowseFolderListener(
        project,
        FileChooserDescriptorFactoryCompat
          .createSingleFileDescriptor()
          .withTitle(title),
      )
      whenTextChanged {
        if (text.isNotBlank()) {
          val newPath = Path(text)
          currentProjectSettings = currentProjectSettings.withNewBuildifierExecutablePath(newPath)
        }
      }
    }

  private fun buildifierExecutableValidationInfo(): ValidationInfo? =
    BuildifierUtil.validateBuildifierExecutable(
      buildifierExecutablePathField.text.takeIf { it.isNotBlank() } ?: BuildifierUtil.detectBuildifierExecutable()?.absolutePath,
    )

  private fun initRunBuildifierOnSaveCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.run.buildifier.on.save.checkbox.text")).apply {
      isSelected = currentProjectSettings.runBuildifierOnSave
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(runBuildifierOnSave = isSelected)
      }
    }

  private fun initShowExcludedDirectoriesAsSeparateNodeCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.show.excluded.directories.as.separate.node.checkbox.text")).apply {
      isSelected = currentProjectSettings.showExcludedDirectoriesAsSeparateNode
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(showExcludedDirectoriesAsSeparateNode = isSelected)
      }
    }

  override fun createComponent(): JComponent =
    panel {
      row(BazelPluginBundle.message("project.settings.project.view.label")) { cell(projectViewPathField).align(Align.FILL) }
      row(BazelPluginBundle.message("project.settings.buildifier.label")) {
        cell(buildifierExecutablePathField).align(Align.FILL).validationInfo { buildifierExecutableValidationInfo() }
      }
      row { cell(runBuildifierOnSaveCheckBox).align(Align.FILL) }
      row { cell(showExcludedDirectoriesAsSeparateNodeCheckBox).align(Align.FILL) }
    }

  override fun isModified(): Boolean = currentProjectSettings != project.bazelProjectSettings

  override fun apply() {
    val isProjectViewPathChanged = currentProjectSettings.projectViewPath != project.bazelProjectSettings.projectViewPath
    val showExcludedDirectoriesAsSeparateNodeChanged =
      currentProjectSettings.showExcludedDirectoriesAsSeparateNode != project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    project.bazelProjectSettings = currentProjectSettings

    if (isProjectViewPathChanged) {
      BazelCoroutineService.getInstance(project).start {
        ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
      }
    }
    if (showExcludedDirectoriesAsSeparateNodeChanged) {
      ProjectView.getInstance(project).refresh()
    }
  }

  override fun reset() {
    super.reset()
    projectViewPathField.text = savedProjectViewPath()
    buildifierExecutablePathField.text = getBuildifierExecPathPlaceholderMessage()
    runBuildifierOnSaveCheckBox.isSelected = project.bazelProjectSettings.runBuildifierOnSave

    showExcludedDirectoriesAsSeparateNodeCheckBox.isSelected = project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    currentProjectSettings = project.bazelProjectSettings
  }

  private fun getBuildifierExecPathPlaceholderMessage(): String =
    currentProjectSettings.getBuildifierPathString()
      ?: BazelPluginBundle.message("buildifier.executable.not.found", if (SystemInfo.isWindows) 0 else 1)

  private fun savedProjectViewPath() =
    project.bazelProjectSettings.projectViewPath
      ?.pathString
      .orEmpty()

  override fun getDisplayName(): String = BazelPluginBundle.message(DISPLAY_NAME_KEY)

  override fun getId(): String = ID

  override fun disposeUIResources() {
    projectViewPathField.dispose()
    super.disposeUIResources()
  }

  companion object {
    const val ID = "bazel.project.settings"
    const val DISPLAY_NAME_KEY = "project.settings.display.name"
  }

  object SearchIndex { // the companion object of a Configurable is not allowed to have non-const members
    val keys =
      listOf(
        "project.settings.buildifier.label",
        "project.settings.project.view.label",
        "project.settings.plugin.show.excluded.directories.as.separate.node.checkbox.text",
        "project.settings.plugin.title",
        "project.settings.plugin.run.buildifier.on.save.checkbox.text",
      )
  }
}

internal class BazelProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelProjectSettingsConfigurable(project)
}
