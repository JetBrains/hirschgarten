package org.jetbrains.bazel.settings

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.openapi.updateSettings.impl.UpdateSettings

const val BAZEL_PLUGIN_ID = "org.jetbrains.bazel"
const val BSP_PLUGIN_ID = "org.jetbrains.bsp"

@Suppress("UnstableApiUsage")
internal object BazelPluginUpdater {
  fun updatePlugins() {
    UpdateChecker.updateAndShowResult(null, UpdateSettings.getInstance())
  }

  fun updatePluginsHosts(newUpdateChannel: UpdateChannel) {
    val currentUpdateChannel = BazelApplicationSettingsService.getInstance().settings.updateChannel
    updatePluginHost(BAZEL_PLUGIN_ID, newUpdateChannel, currentUpdateChannel)
    updatePluginHost(BSP_PLUGIN_ID, newUpdateChannel, currentUpdateChannel)
  }

  private fun updatePluginHost(
    id: String,
    newUpdateChannel: UpdateChannel,
    currentUpdateChannel: UpdateChannel,
  ) {
    if (newUpdateChannel == currentUpdateChannel) return
    // go from a presumably higher version (e.g., nightly) to a smaller version (e.g., release)
    if (newUpdateChannel < currentUpdateChannel) {
      getPluginDescriptorForId(id)?.let { patchPluginVersion("0.0.0", it) }
    }
    val updateSettings = UpdateSettings.getInstance()
    val nightlyRepo = UpdateChannel.NIGHTLY.getPluginUrlFromId(id)
    updateSettings.storedPluginHosts.remove(nightlyRepo)

    when (newUpdateChannel) {
      UpdateChannel.NIGHTLY -> updateSettings.storedPluginHosts.add(nightlyRepo)
      else -> {}
    }
  }

  /**
   * This function is used to force-downgrade a plugin version
   */
  fun patchPluginVersion(newVersion: String, descriptor: IdeaPluginDescriptorImpl) {
    val versionField = IdeaPluginDescriptorImpl::class.java.getDeclaredField("version")
    versionField.setAccessible(true)
    versionField.set(descriptor, newVersion)
    versionField.setAccessible(false)
  }

  fun getPluginDescriptorForId(id: String): IdeaPluginDescriptorImpl? =
    PluginManagerCore.getPlugin(getPluginId(id)) as? IdeaPluginDescriptorImpl

  private fun getPluginId(pluginIdString: String): PluginId? = PluginId.findId(pluginIdString)
}
