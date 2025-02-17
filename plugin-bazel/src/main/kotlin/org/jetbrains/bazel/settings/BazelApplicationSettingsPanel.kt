package org.jetbrains.bazel.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelPluginBundle
import javax.swing.JComponent

@Service(Service.Level.APP)
@State(
  name = "BazelApplicationSettingsService",
  storages = [
    Storage(
      "bazel.application.settings.xml",
      roamingType = RoamingType.DISABLED,
    ),
  ],
)
internal class BazelApplicationSettingsService : PersistentStateComponent<BazelApplicationSettings> {
  var settings: BazelApplicationSettings = BazelApplicationSettings()

  override fun getState(): BazelApplicationSettings? = settings

  override fun loadState(state: BazelApplicationSettings) {
    settings = state
  }

  companion object {
    @JvmStatic
    fun getInstance(): BazelApplicationSettingsService = service()
  }
}

internal data class BazelApplicationSettings(var updateChannel: UpdateChannel = UpdateChannel.RELEASE)

class BazelApplicationSettingsConfigurable : SearchableConfigurable {
  private val panelApplicationSettings = BazelApplicationSettingsService.getInstance().settings.copy()

  private val updateChannelComboBox: ComboBox<UpdateChannel> = ComboBox<UpdateChannel>()

  init {
    initUpdateChannelComboBox()
  }

  private fun initUpdateChannelComboBox() {
    updateChannelComboBox.model = CollectionComboBoxModel(UpdateChannel.entries)
    updateChannelComboBox.renderer = UpdateChannel.Renderer()
    updateChannelComboBox.selectedItem = panelApplicationSettings.updateChannel
    updateChannelComboBox.addActionListener {
      panelApplicationSettings.updateChannel = updateChannelComboBox.selectedItem as UpdateChannel
    }
  }

  override fun getId(): String = ID

  override fun getDisplayName(): String? = BazelPluginBundle.message(DISPLAY_NAME_KEY)

  override fun createComponent(): JComponent? =
    panel {
      group(BazelPluginBundle.message("application.settings.updates.title"), false) {
        row(BazelPluginBundle.message("application.settings.update.channel.dropdown.title")) {
          cell(updateChannelComboBox).align(Align.FILL).resizableColumn()
          contextHelp(BazelPluginBundle.message("application.settings.update.channel.dropdown.help.description")).align(AlignX.RIGHT)
        }
      }
    }

  override fun isModified(): Boolean = panelApplicationSettings != BazelApplicationSettingsService.getInstance().settings

  override fun apply() {
    BazelPluginUpdater.updatePluginsHosts(panelApplicationSettings.updateChannel)
    BazelPluginUpdater.updatePlugins()

    BazelApplicationSettingsService.getInstance().settings = panelApplicationSettings.copy()
  }

  companion object {
    const val ID = "bazel.application.settings"
    const val DISPLAY_NAME_KEY = "application.settings.display.name"
  }

  object SearchIndex { // the companion object of a Configurable is not allowed to have non-const members
    val keys =
      listOf(
        "application.settings.updates.title",
        "application.settings.update.channel.dropdown.title",
      )
  }
}

internal class BazelApplicationSettingsConfigurableProvider : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelApplicationSettingsConfigurable()
}
