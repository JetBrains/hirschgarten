package org.jetbrains.bazel.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

internal val serverDetectedJdk = ProjectJdkImpl("Server Detected Jdk", JavaSdk.getInstance())

internal data class BazelApplicationServerSettings(
  val selectedJdk: Sdk = serverDetectedJdk,
)

internal data class BazelApplicationSettings(
  var serverSettings: BazelApplicationServerSettings = BazelApplicationServerSettings(),
)

internal class BazelApplicationSettingsPanel : Configurable {
  private val serverJdkComboBoxModel: ProjectSdksModel = ProjectSdksModel()
  private val serverJdkComboBox: JdkComboBox

  private var currentBazelApplicationSettings = bazelApplicationSettings

  init {
    serverJdkComboBoxModel.addSdk(serverDetectedJdk)
    serverJdkComboBoxModel.syncSdks()

    serverJdkComboBox = JdkComboBox(null, serverJdkComboBoxModel, null, null, null, null)
    serverJdkComboBox.whenItemSelected {
      currentBazelApplicationSettings = currentBazelApplicationSettings.newWithSdk(it.jdk) ?: currentBazelApplicationSettings
    }
  }

  private fun BazelApplicationSettings.newWithSdk(sdk: Sdk?): BazelApplicationSettings? =
    sdk?.let {
      copy(
        serverSettings = serverSettings.copy(selectedJdk = it)
      )
    }

  override fun createComponent(): JComponent = panel {
    group("Server Settings", true) {
      row("JDK used to run server") { cell(serverJdkComboBox) }
    }
  }

  override fun isModified(): Boolean = bazelApplicationSettings != currentBazelApplicationSettings

  override fun apply() {
    bazelApplicationSettings = currentBazelApplicationSettings
  }

  override fun reset() {
    super.reset()

    serverJdkComboBox.selectedJdk = savedJdkOrDefault()
    currentBazelApplicationSettings = bazelApplicationSettings
  }

  private fun savedJdkOrDefault(): Sdk =
    serverJdkComboBoxModel.findSdk(bazelApplicationSettings.serverSettings.selectedJdk.name) ?: serverDetectedJdk

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
  private set(value) { BazelApplicationSettingsService.getInstance().settings = value }
