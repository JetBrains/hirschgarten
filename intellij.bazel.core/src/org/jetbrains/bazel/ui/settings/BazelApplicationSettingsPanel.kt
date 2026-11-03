package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.BazelApplicationSettingsService
import org.jetbrains.bazel.settings.bazel.UpdateChannel
import javax.swing.JComponent

internal class BazelApplicationSettingsConfigurable : SearchableConfigurable {
  private val panelApplicationSettings = BazelApplicationSettingsService.getInstance().settings.copy()

  private val updateChannelComboBox: ComboBox<UpdateChannel> = ComboBox<UpdateChannel>()
  private val enablePhasedSyncCheckBox: JBCheckBox = initEnablePhasedSyncCheckBox()

  init {
    initUpdateChannelComboBox()
  }

  private fun initUpdateChannelComboBox() {
    updateChannelComboBox.model = CollectionComboBoxModel(UpdateChannel.entries)
    updateChannelComboBox.renderer = textListCellRenderer("") { it.displayName }
    updateChannelComboBox.selectedItem = panelApplicationSettings.updateChannel
    updateChannelComboBox.addActionListener {
      panelApplicationSettings.updateChannel = updateChannelComboBox.selectedItem as UpdateChannel
    }
  }

  private fun initEnablePhasedSyncCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("application.settings.plugin.enable.phased.sync.checkbox.text")).apply {
      val settings = panelApplicationSettings
      isSelected = settings.enablePhasedSync
      addItemListener {
        settings.enablePhasedSync = isSelected
      }
    }

  override fun getId(): String = ID

  override fun getDisplayName(): String = BazelPluginBundle.message(DISPLAY_NAME_KEY)

  override fun createComponent(): JComponent =
    panel {
      group(BazelPluginBundle.message("application.settings.updates.title")) {
        row(BazelPluginBundle.message("application.settings.update.channel.dropdown.title")) {
          cell(updateChannelComboBox)
            .contextHelp(BazelPluginBundle.message("application.settings.update.channel.dropdown.help.description"))
        }
      }
      group(BazelPluginBundle.message("application.settings.plugin.experimental.settings")) {
        row {
          cell(enablePhasedSyncCheckBox)
            .contextHelp(BazelPluginBundle.message("application.settings.plugin.enable.phased.sync.help.text"))
        }
      }
    }

  override fun isModified(): Boolean = panelApplicationSettings != BazelApplicationSettingsService.getInstance().settings

  override fun apply() {
    BazelPluginUpdater.updatePluginsHost(panelApplicationSettings.updateChannel)
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
