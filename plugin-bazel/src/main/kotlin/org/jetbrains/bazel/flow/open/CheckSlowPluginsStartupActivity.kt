package org.jetbrains.bazel.flow.open

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.plugins.bsp.startup.BspProjectActivity
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier

// See https://youtrack.jetbrains.com/issue/BAZEL-1236
private val SLOW_PLUGINS =
  listOf(
    // Spring is slow, but oftentimes necessary
    // "com.intellij.spring",
    "com.intellij.lang.jsgraphql",
    "com.intellij.python.django",
  )

internal class CheckSlowPluginsStartupActivity : BspProjectActivity() {
  override suspend fun Project.executeForBspProject() {
    val plugins = PluginManagerCore.loadedPlugins
    val slowPlugins =
      plugins.filter {
        it.pluginId.idString in SLOW_PLUGINS
      }
    if (slowPlugins.isEmpty()) return
    BspBalloonNotifier.warn(
      BazelPluginBundle.message("widget.check.slow.pluging.title", slowPlugins.size),
      BazelPluginBundle.message(
        "widget.check.slow.plugins.message",
        slowPlugins.joinToString(", ") { it.name },
        slowPlugins.size,
      ),
    )
  }
}
