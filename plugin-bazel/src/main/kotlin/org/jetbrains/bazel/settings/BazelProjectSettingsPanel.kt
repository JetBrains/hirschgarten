package org.jetbrains.bazel.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.DumbAware
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
import org.jetbrains.plugins.bsp.config.defaultJdkName
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.flow.sync.SecondPhaseSync
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.pathString

private const val BAZEL_PROJECT_SETTINGS_ID = "bazel.project.settings"
const val BAZEL_PROJECT_SETTINGS_DISPLAY_NAME = "Bazel"

data class BazelProjectSettings(
  val projectViewPath: Path? = null,
  val selectedServerJdkName: String? = null,
  val customJvmOptions: List<String> = emptyList(),
  val hotSwapEnabled: Boolean = true,
) {
  fun withNewProjectViewPath(newProjectViewFilePath: Path): BazelProjectSettings = copy(projectViewPath = newProjectViewFilePath)

  fun withNewServerJdkName(newServerJdkName: String?): BazelProjectSettings? =
    newServerJdkName?.let {
      copy(selectedServerJdkName = newServerJdkName)
    }

  fun withNewCustomJvmOptions(newCustomJvmOptions: List<String>): BazelProjectSettings = copy(customJvmOptions = newCustomJvmOptions)

  fun withNewHotSwapEnabled(newHotSwapEnabled: Boolean): BazelProjectSettings = copy(hotSwapEnabled = newHotSwapEnabled)
}

internal data class BazelProjectSettingsState(
  var projectViewPathUri: String? = null,
  var selectedServerJdkName: String? = null,
  var customJvmOptions: List<String> = emptyList(),
  var hotSwapEnabled: Boolean = true,
) {
  fun isEmptyState(): Boolean = this == BazelProjectSettingsState()
}

@State(
  name = "BazelProjectSettingsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelProjectSettingsService :
  DumbAware,
  PersistentStateComponent<BazelProjectSettingsState> {
  var settings: BazelProjectSettings = BazelProjectSettings()

  override fun getState(): BazelProjectSettingsState =
    BazelProjectSettingsState(
      projectViewPathUri = settings.projectViewPath?.toUri()?.toString(),
      selectedServerJdkName = settings.selectedServerJdkName,
      customJvmOptions = settings.customJvmOptions,
      hotSwapEnabled = settings.hotSwapEnabled,
    )

  override fun loadState(settingsState: BazelProjectSettingsState) {
    if (!settingsState.isEmptyState()) {
      this.settings =
        BazelProjectSettings(
          projectViewPath =
            settingsState.projectViewPathUri?.takeIf { it.isNotBlank() }?.let {
              Paths.get(
                URI(it),
              )
            },
          selectedServerJdkName = settingsState.selectedServerJdkName,
          customJvmOptions = settingsState.customJvmOptions,
          hotSwapEnabled = settingsState.hotSwapEnabled,
        )
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelProjectSettingsService = project.getService(BazelProjectSettingsService::class.java)
  }
}

internal class BazelProjectSettingsConfigurable(private val project: Project) : SearchableConfigurable {
  private val projectViewPathField: TextFieldWithBrowseButton

  private val serverJdkComboBoxModel: ProjectSdksModel = ProjectSdksModel()
  private val serverJdkComboBox: JdkComboBox

  private val serverCustomJvmOptions: RawCommandLineEditor

  private val hotswapEnabledCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    serverJdkComboBoxModel.syncSdks()
    serverJdkComboBox = initServerJdkComboBox()
    serverCustomJvmOptions = initServerCustomJvmOptions()

    projectViewPathField = initProjectViewFileField()
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()
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
      }
    }

  override fun isModified(): Boolean = currentProjectSettings != project.bazelProjectSettings

  override fun apply() {
    val isProjectViewPathChanged = currentProjectSettings.projectViewPath != project.bazelProjectSettings.projectViewPath
    project.bazelProjectSettings = currentProjectSettings
    if (isProjectViewPathChanged) {
      BspCoroutineService.getInstance(project).start {
        ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
      }
    }
  }

  override fun reset() {
    super.reset()
    projectViewPathField.text = savedProjectViewPath()

    ApplicationManager.getApplication().runWriteAction {
      serverJdkComboBox.selectedJdk = savedJdkOrDefault()
    }

    serverCustomJvmOptions.text = savedCustomJvmOptions()

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

var Project.bazelProjectSettings: BazelProjectSettings
  get() = BazelProjectSettingsService.getInstance(this).settings.copy()
  set(value) {
    BazelProjectSettingsService.getInstance(this).settings = value.copy()
  }
