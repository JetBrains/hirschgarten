package org.jetbrains.bazel.ui.settings

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginMainDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.openapi.updateSettings.impl.UpdateSettings

const val BAZEL_PLUGIN_ID = "org.jetbrains.bazel"

@Suppress("UnstableApiUsage")
object BazelPluginUpdater {
  fun updatePlugins() {
    UpdateChecker.updateAndShowResult(null, UpdateSettings.getInstance())
  }

  fun updatePluginsHost(newUpdateChannel: UpdateChannel) {
    val currentUpdateChannel = BazelApplicationSettingsService.getInstance().settings.updateChannel
    updatePluginHost(newUpdateChannel, currentUpdateChannel)
  }

  private fun updatePluginHost(newUpdateChannel: UpdateChannel, currentUpdateChannel: UpdateChannel) {
    if (newUpdateChannel == currentUpdateChannel) return
    // go from a presumably higher version (e.g., nightly) to a smaller version (e.g., release)
    if (newUpdateChannel < currentUpdateChannel) {
      getPluginDescriptorForId(BAZEL_PLUGIN_ID)?.let { patchPluginVersion("0.0.0", it) }
    }
    val updateSettings = UpdateSettings.getInstance()
    val nightlyRepo = UpdateChannel.NIGHTLY.getPluginUrlFromId(BAZEL_PLUGIN_ID)
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
    if (descriptor !is PluginMainDescriptor) throw IllegalArgumentException("Unsupported descriptor type: ${descriptor.javaClass.name}")

    val versionField = PluginMainDescriptor::class.java.getDeclaredField("version")
    try {
      versionField.setAccessible(true)
      versionField.set(descriptor, newVersion)
    }
    finally {
      versionField.setAccessible(false)
    }
  }

  fun getPluginDescriptorForId(id: String): IdeaPluginDescriptorImpl? =
    PluginManagerCore.getPlugin(getPluginId(id)) as? IdeaPluginDescriptorImpl

  private fun getPluginId(pluginIdString: String): PluginId? = PluginId.findId(pluginIdString)
}
