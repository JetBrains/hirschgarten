package org.jetbrains.bazel.settings

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
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.bsp.connection.stateService
import org.jetbrains.bazel.config.BazelPluginBundle
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.pathString

private const val BAZEL_PROJECT_SETTINGS_ID = "bazel.project.settings"
private const val JAVA_SDK_TYPE = "JavaSDK"

internal val serverDetectedJdk = ProjectJdkImpl("Server Detected Jdk", JavaSdk.getInstance())

data class BazelProjectSettings(
  val projectViewPath: Path? = null,
  val selectedJdk: Sdk = serverDetectedJdk,
  val customJvmOptions: List<String> = emptyList(),
) {
  fun withNewProjectViewPath(newProjectViewFilePath: Path): BazelProjectSettings = copy(projectViewPath = newProjectViewFilePath)

  fun withNewSdk(newSdk: Sdk?): BazelProjectSettings? = newSdk?.let { copy(selectedJdk = newSdk) }

  fun withNewCustomJvmOptions(newCustomJvmOptions: List<String>): BazelProjectSettings = copy(customJvmOptions = newCustomJvmOptions)
}

internal data class BazelProjectSettingsState(
  var projectViewPathUri: String? = null,
  var sdkName: String? = null,
  var customJvmOptions: List<String> = emptyList(),
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
      sdkName = settings.selectedJdk.name,
      customJvmOptions = settings.customJvmOptions,
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
          selectedJdk = settingsState.sdkName?.let { ProjectJdkTable.getInstance().findJdk(it, JAVA_SDK_TYPE) } ?: serverDetectedJdk,
          customJvmOptions = settingsState.customJvmOptions,
        )
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelProjectSettingsService = project.getService(BazelProjectSettingsService::class.java)
  }
}

class BazelProjectSettingsConfigurable(private val project: Project) : SearchableConfigurable {
  private val projectViewPathField: TextFieldWithBrowseButton

  private val serverJdkComboBoxModel: ProjectSdksModel = ProjectSdksModel()
  private val serverJdkComboBox: JdkComboBox

  private val serverCustomJvmOptions: RawCommandLineEditor

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    serverJdkComboBoxModel.addSdk(serverDetectedJdk)
    serverJdkComboBoxModel.syncSdks()

    serverJdkComboBox = initServerJdkComboBox()
    serverCustomJvmOptions = initServerCustomJvmOptions()

    projectViewPathField = initProjectViewFileField()
  }

  private fun initServerJdkComboBox(): JdkComboBox =
    JdkComboBox(project, serverJdkComboBoxModel, null, null, null, null).apply {
      whenItemSelected {
        currentProjectSettings = currentProjectSettings.withNewSdk(it.jdk) ?: currentProjectSettings
      }
    }

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
        textField.addBrowseFolderListener(
          "Select Path",
          "Select the path for your project view file.",
          project,
          FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(),
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

  override fun createComponent(): JComponent =
    panel {
      group(BazelPluginBundle.message("project.settings.server.title"), true) {
        row(BazelPluginBundle.message("project.settings.project.view.label")) { cell(projectViewPathField).align(Align.FILL) }
        row(BazelPluginBundle.message("project.settings.server.jdk.label")) { cell(serverJdkComboBox).align(Align.FILL) }
        row(BazelPluginBundle.message("project.settings.server.jvm.options.label")) { cell(serverCustomJvmOptions).align(Align.FILL) }
      }
    }

  override fun isModified(): Boolean = currentProjectSettings != project.bazelProjectSettings

  override fun apply() {
    project.bazelProjectSettings = currentProjectSettings
  }

  override fun reset() {
    super.reset()
    projectViewPathField.text = savedProjectViewPath()

    serverJdkComboBox.selectedJdk = savedJdkOrDefault()
    serverCustomJvmOptions.text = savedCustomJvmOptions()

    currentProjectSettings = project.bazelProjectSettings
  }

  private fun savedProjectViewPath(): String =
    project.bazelProjectSettings
      .projectViewPath
      ?.pathString
      .orEmpty()

  private fun savedJdkOrDefault(): Sdk =
    project.bazelProjectSettings
      .selectedJdk
      .name
      .let { serverJdkComboBoxModel.findSdk(it) } ?: serverDetectedJdk

  private fun savedCustomJvmOptions(): String =
    project.bazelProjectSettings
      .customJvmOptions
      .joinToString("\n")

  override fun getDisplayName(): String = BazelPluginBundle.message("project.settings.display.name")

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
  get() = BazelProjectSettingsService.getInstance(this).settings
  set(value) {
    BazelProjectSettingsService.getInstance(this).settings = value
  }
