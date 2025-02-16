package org.jetbrains.bazel.ui.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.defaultJdkName
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.settings.stateService
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.pathString

private const val BAZEL_PROJECT_SETTINGS_ID = "bazel.project.settings"
const val BAZEL_PROJECT_SETTINGS_DISPLAY_NAME = "Bazel"

internal class BazelProjectSettingsConfigurable(private val project: Project) : SearchableConfigurable {
  private val projectViewPathField: TextFieldWithBrowseButton

  private val serverJdkComboBoxModel: ProjectSdksModel = ProjectSdksModel()
  private val serverJdkComboBox: JdkComboBox

  private val serverCustomJvmOptions: RawCommandLineEditor

  private val hotswapEnabledCheckBox: JBCheckBox
  private val showExcludedDirectoriesAsSeparateNodeCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    serverJdkComboBoxModel.syncSdks()
    serverJdkComboBox = initServerJdkComboBox()
    serverCustomJvmOptions = initServerCustomJvmOptions()

    projectViewPathField = initProjectViewFileField()
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()
    showExcludedDirectoriesAsSeparateNodeCheckBox = initShowExcludedDirectoriesAsSeparateNodeCheckBox()
  }

  private fun initServerJdkComboBox(): JdkComboBox =
    JdkComboBox(project, serverJdkComboBoxModel, ::isEligibleJdk, null, null, null).apply {
      whenItemSelected {
        currentProjectSettings = currentProjectSettings.withNewServerJdkName(it.jdk?.name) ?: currentProjectSettings
      }
    }

  private fun isEligibleJdk(type: SdkTypeId) = JavaSdk.getInstance().equals(type)

  private fun initServerCustomJvmOptions(): RawCommandLineEditor =
    RawCommandLineEditor().apply {
      textField.whenTextChanged {
        currentProjectSettings = currentProjectSettings.withNewCustomJvmOptions(text.toJvmOptions())
      }
    }

  private fun String.toJvmOptions(): List<String> = split("\\s+".toRegex()).filter { it.isNotBlank() }

  private fun initProjectViewFileField(): TextFieldWithBrowseButton =
    TextFieldWithBrowseButton()
      .also { textField ->
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
        textField.setEnabledAndHint()
      }

  private fun TextFieldWithBrowseButton.setEnabledAndHint() {
    fun applyChange() {
      // project view file can only be set when connection file is not used
      isEnabled = project.stateService.connectionFile == null
    }
    applyChange()
    addPropertyChangeListener("enabled") { _ ->
      applyChange()
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
      group(BazelPluginBundle.message("project.settings.server.title"), false) {
        row(BazelPluginBundle.message("project.settings.project.view.label")) { cell(projectViewPathField).align(Align.FILL) }
        row(BazelPluginBundle.message("project.settings.server.jdk.label")) {
          cell(serverJdkComboBox).align(Align.FILL).resizableColumn()
          contextHelp(BazelPluginBundle.message("project.settings.server.jdk.context.help.description")).align(AlignX.RIGHT)
        }
        row(BazelPluginBundle.message("project.settings.server.jvm.options.label")) { cell(serverCustomJvmOptions).align(Align.FILL) }
      }
      group(BazelPluginBundle.message("project.settings.plugin.title"), false) {
        row { cell(hotswapEnabledCheckBox).align(Align.FILL) }
        row { cell(showExcludedDirectoriesAsSeparateNodeCheckBox).align(Align.FILL) }
      }
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

    ApplicationManager.getApplication().runWriteAction {
      serverJdkComboBox.selectedJdk = savedJdkOrDefault()
    }

    serverCustomJvmOptions.text = savedCustomJvmOptions()

    showExcludedDirectoriesAsSeparateNodeCheckBox.isSelected = project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    currentProjectSettings = project.bazelProjectSettings
  }

  private fun savedProjectViewPath(): String =
    project.bazelProjectSettings
      .projectViewPath
      ?.pathString
      .orEmpty()

  private fun savedJdkOrDefault(): Sdk? =
    (project.bazelProjectSettings.selectedServerJdkName ?: project.defaultJdkName) ?.let {
      serverJdkComboBoxModel.findSdk(it)
    }

  private fun savedCustomJvmOptions(): String =
    project.bazelProjectSettings
      .customJvmOptions
      .joinToString("\n")

  override fun getDisplayName(): String = BAZEL_PROJECT_SETTINGS_DISPLAY_NAME

  override fun getId(): String = BAZEL_PROJECT_SETTINGS_ID

  override fun disposeUIResources() {
    serverJdkComboBoxModel.disposeUIResources()
    projectViewPathField.dispose()
    super.disposeUIResources()
  }
}

internal class BazelProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelProjectSettingsConfigurable(project)
}
