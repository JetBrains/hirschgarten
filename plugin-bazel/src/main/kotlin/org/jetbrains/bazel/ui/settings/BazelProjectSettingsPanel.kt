package org.jetbrains.bazel.ui.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.pathString

class BazelProjectSettingsConfigurable(private val project: Project) : SearchableConfigurable {
  private val projectViewPathField: TextFieldWithBrowseButton
  private val hotswapEnabledCheckBox: JBCheckBox
  private val showExcludedDirectoriesAsSeparateNodeCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    projectViewPathField = initProjectViewFileField()
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()
    showExcludedDirectoriesAsSeparateNodeCheckBox = initShowExcludedDirectoriesAsSeparateNodeCheckBox()
  }

  private fun initProjectViewFileField(): TextFieldWithBrowseButton =
    TextFieldWithBrowseButton().also { textField ->
      val title = "Select Path"
      val description = "Select the path for your project view file."
      textField.addBrowseFolderListener(
        project,
        FileChooserDescriptorFactory
          .createSingleFileDescriptor()
          .withTitle(
            title,
          ).withDescription(description),
      )
      textField.whenTextChanged {
        val newPath = Path(textField.text)
        currentProjectSettings = currentProjectSettings.withNewProjectViewPath(newPath)
      }
    }

  private fun initHotSwapEnabledCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.hotswap.enabled.checkbox.text")).apply {
      isSelected = currentProjectSettings.hotSwapEnabled
      addItemListener {
        currentProjectSettings = currentProjectSettings.withNewHotSwapEnabled(isSelected)
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
      row { cell(hotswapEnabledCheckBox).align(Align.FILL) }
      row { cell(showExcludedDirectoriesAsSeparateNodeCheckBox).align(Align.FILL) }
    }

  override fun isModified(): Boolean = currentProjectSettings != project.bazelProjectSettings

  override fun apply() {
    val isProjectViewPathChanged = currentProjectSettings.projectViewPath != project.bazelProjectSettings.projectViewPath
    val showExcludedDirectoriesAsSeparateNodeChanged =
      currentProjectSettings.showExcludedDirectoriesAsSeparateNode != project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    project.bazelProjectSettings = currentProjectSettings

    if (isProjectViewPathChanged) {
      BspCoroutineService.getInstance(project).start {
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

    showExcludedDirectoriesAsSeparateNodeCheckBox.isSelected = project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    currentProjectSettings = project.bazelProjectSettings
  }

  private fun savedProjectViewPath(): String =
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
        "project.settings.server.title",
        "project.settings.project.view.label",
        "project.settings.server.jdk.label",
        "project.settings.server.jvm.options.label",
        "project.settings.plugin.title",
        "project.settings.plugin.hotswap.enabled.checkbox.text",
        "project.settings.plugin.show.excluded.directories.as.separate.node.checkbox.text",
      )
  }
}

internal class BazelProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelProjectSettingsConfigurable(project)
}
