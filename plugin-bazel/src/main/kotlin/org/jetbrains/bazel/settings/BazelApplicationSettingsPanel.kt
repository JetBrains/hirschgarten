package org.jetbrains.bazel.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

internal val serverDetectedJdk = ProjectJdkImpl("Server Detected Jdk", JavaSdk.getInstance())

internal data class BazelApplicationServerSettings(
    val selectedJdk: Sdk = serverDetectedJdk,
    val customJvmOptions: List<String> = emptyList(),
)

internal data class BazelApplicationSettings(
    var serverSettings: BazelApplicationServerSettings = BazelApplicationServerSettings(),
) {
  internal fun newWithSdk(sdk: Sdk?): BazelApplicationSettings? =
      sdk?.let { copy(serverSettings = serverSettings.copy(selectedJdk = it)) }

  internal fun newWithCustomJvmOptions(customJvmOptions: List<String>): BazelApplicationSettings =
      copy(serverSettings = serverSettings.copy(customJvmOptions = customJvmOptions))
}

internal class BazelApplicationSettingsPanel : Configurable {
  private val serverJdkComboBoxModel: ProjectSdksModel = ProjectSdksModel()
  private val serverJdkComboBox: JdkComboBox

  private val serverCustomJvmOptions: RawCommandLineEditor

  private var currentBazelApplicationSettings = bazelApplicationSettings

  init {
    serverJdkComboBoxModel.addSdk(serverDetectedJdk)
    serverJdkComboBoxModel.syncSdks()

    serverJdkComboBox = initServerJdkComboBox()
    serverCustomJvmOptions = initServerCustomJvmOptions()
  }

  private fun initServerJdkComboBox(): JdkComboBox =
      JdkComboBox(null, serverJdkComboBoxModel, null, null, null, null).apply {
        whenItemSelected {
          currentBazelApplicationSettings =
              currentBazelApplicationSettings.newWithSdk(it.jdk) ?: currentBazelApplicationSettings
        }
      }

  private fun initServerCustomJvmOptions(): RawCommandLineEditor =
      RawCommandLineEditor().apply {
        textField.whenTextChanged {
          currentBazelApplicationSettings =
              currentBazelApplicationSettings.newWithCustomJvmOptions(text.toJvmOptions())
        }
      }

  private fun String.toJvmOptions(): List<String> =
      split("\\s+".toRegex()).filter { it.isNotBlank() }

  override fun createComponent(): JComponent = panel {
    group("Server Settings", true) {
      row("JDK used to run server") { cell(serverJdkComboBox).align(Align.FILL) }
      row("Server Custom JVM options") { cell(serverCustomJvmOptions).align(Align.FILL) }
    }
  }

  override fun isModified(): Boolean = bazelApplicationSettings != currentBazelApplicationSettings

  override fun apply() {
    bazelApplicationSettings = currentBazelApplicationSettings
  }

  override fun reset() {
    super.reset()

    serverJdkComboBox.selectedJdk = savedJdkOrDefault()
    serverCustomJvmOptions.text = savedCustomJvmOptions()

    currentBazelApplicationSettings = bazelApplicationSettings
  }

  private fun savedJdkOrDefault(): Sdk =
      serverJdkComboBoxModel.findSdk(bazelApplicationSettings.serverSettings.selectedJdk.name)
          ?: serverDetectedJdk

  private fun savedCustomJvmOptions(): String =
      bazelApplicationSettings.serverSettings.customJvmOptions.joinToString("\n")

  override fun getDisplayName(): String = "Bazel"
}

@Service(Service.Level.APP)
internal class BazelApplicationSettingsService {
  var settings = BazelApplicationSettings()

  companion object {
    @JvmStatic
    fun getInstance(): BazelApplicationSettingsService = service<BazelApplicationSettingsService>()
  }
}

internal var bazelApplicationSettings: BazelApplicationSettings
  get() = BazelApplicationSettingsService.getInstance().settings
  private set(value) {
    BazelApplicationSettingsService.getInstance().settings = value
  }
