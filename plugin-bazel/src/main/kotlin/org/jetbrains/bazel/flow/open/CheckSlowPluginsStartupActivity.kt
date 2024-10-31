package org.jetbrains.bazel.flow.open

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.newui.MyPluginModel
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.plugins.bsp.startup.BspProjectActivity

// See https://youtrack.jetbrains.com/issue/BAZEL-1236
private val SLOW_PLUGINS =
  listOf(
    "com.intellij.spring",
    "com.intellij.python.django",
  )

private const val NOTIFICATION_GROUP = "Bazel slow plugins warning"

internal class CheckSlowPluginsStartupActivity : BspProjectActivity() {
  override suspend fun Project.executeForBspProject() {
    val plugins = PluginManagerCore.loadedPlugins
    val slowPlugins =
      plugins.filter {
        it.pluginId.idString in SLOW_PLUGINS
      }
    if (slowPlugins.isEmpty()) return
    NotificationGroupManager
      .getInstance()
      .getNotificationGroup(NOTIFICATION_GROUP)
      .createNotification(
        BazelPluginBundle.message("widget.check.slow.plugins.title", slowPlugins.size),
        BazelPluginBundle.message(
          "widget.check.slow.plugins.message",
          slowPlugins.joinToString(", ") { it.name },
          slowPlugins.size,
        ),
        NotificationType.WARNING,
      ).addAction(
        NotificationAction.createExpiring(BazelPluginBundle.message("widget.check.slow.plugins.action", slowPlugins.size)) { _, _ ->
          disablePlugins(this, slowPlugins)
        },
      ).notify(this)
  }

  private fun disablePlugins(project: Project, plugins: List<IdeaPluginDescriptor>) {
    val model = MyPluginModel(project)
    model.disable(plugins)
    model.apply(null)
  }
}
