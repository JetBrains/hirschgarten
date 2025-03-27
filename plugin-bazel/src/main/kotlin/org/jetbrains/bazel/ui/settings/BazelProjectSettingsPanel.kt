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
import org.jetbrains.bazel.coroutines.BazelCoroutineService
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

  // experimental features
  private val enableLocalJvmActionsCheckBox: JBCheckBox
  private val useIntellijTestRunnerCheckBox: JBCheckBox
  private val enableBuildWithJpsCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    projectViewPathField = initProjectViewFileField()
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()
    showExcludedDirectoriesAsSeparateNodeCheckBox = initShowExcludedDirectoriesAsSeparateNodeCheckBox()

    // TODO: BAZEL-1837
    // experimental features
    useIntellijTestRunnerCheckBox = initUseIntellijTestRunnerCheckBoxBox()
    enableLocalJvmActionsCheckBox = initEnableLocalJvmActionsCheckBox()

    enableBuildWithJpsCheckBox = initEnableBuildWithJpsCheckBox()
  }

  private fun initEnableLocalJvmActionsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.local.jvm.actions.checkbox.text")).apply {
      isSelected = currentProjectSettings.enableLocalJvmActions
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(enableLocalJvmActions = isSelected)
        useIntellijTestRunnerCheckBox.isEnabled = isSelected
      }
    }

  private fun initUseIntellijTestRunnerCheckBoxBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.use.intellij.test.runner.checkbox.text")).apply {
      isSelected = currentProjectSettings.useIntellijTestRunner
      isEnabled = currentProjectSettings.enableLocalJvmActions
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(useIntellijTestRunner = isSelected)
      }
    }

  private fun initEnableBuildWithJpsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.build.with.jps.checkbox.text")).apply {
      isSelected = currentProjectSettings.enableBuildWithJps
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(enableBuildWithJps = isSelected)
      }
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
      group(BazelPluginBundle.message("project.settings.general.settings")) {
        row(BazelPluginBundle.message("project.settings.project.view.label")) { cell(projectViewPathField).align(Align.FILL) }
        row { cell(hotswapEnabledCheckBox).align(Align.FILL) }
        row { cell(showExcludedDirectoriesAsSeparateNodeCheckBox).align(Align.FILL) }
      }
      group(BazelPluginBundle.message("project.settings.experimental.settings")) {
        group(BazelPluginBundle.message("project.settings.local.runner.settings")) {
          row { cell(enableLocalJvmActionsCheckBox).align(Align.FILL) }
          row {
            cell(useIntellijTestRunnerCheckBox).align(Align.FILL)
            contextHelp(BazelPluginBundle.message("project.settings.plugin.use.intellij.test.runner.help.text"))
          }
        }

        row { cell(enableBuildWithJpsCheckBox).align(Align.FILL) }
      }
    }

  override fun isModified(): Boolean = currentProjectSettings != project.bazelProjectSettings

  override fun apply() {
    val isProjectViewPathChanged = currentProjectSettings.projectViewPath != project.bazelProjectSettings.projectViewPath
    val isEnableBuildWithJpsChanged = currentProjectSettings.enableBuildWithJps != project.bazelProjectSettings.enableBuildWithJps
    val showExcludedDirectoriesAsSeparateNodeChanged =
      currentProjectSettings.showExcludedDirectoriesAsSeparateNode != project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    project.bazelProjectSettings = currentProjectSettings

    if (isProjectViewPathChanged || isEnableBuildWithJpsChanged) {
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
        "project.settings.plugin.enable.local.jvm.actions.checkbox.text",
        "project.settings.plugin.hotswap.enabled.checkbox.text",
        "project.settings.plugin.show.excluded.directories.as.separate.node.checkbox.text",
        "project.settings.plugin.title",
        "project.settings.plugin.use.intellij.test.runner.checkbox.text",
      )
  }
}

internal class BazelProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelProjectSettingsConfigurable(project)
}
