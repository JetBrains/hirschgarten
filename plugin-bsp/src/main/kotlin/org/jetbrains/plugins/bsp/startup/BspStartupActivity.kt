package org.jetbrains.plugins.bsp.startup

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.newui.MyPluginModel
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

private const val NOTIFICATION_GROUP = "Disable BSP plugin"

class BspStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val bspPlugin = PluginManagerCore.loadedPlugins.first { it.pluginId.idString == "org.jetbrains.bsp" }
    NotificationGroupManager
      .getInstance()
      .getNotificationGroup(NOTIFICATION_GROUP)
      .createNotification(
        "BSP and Bazel plugins have been merged",
        "Please use the Bazel (EAP) plugin instead from now on. The BSP plugin will no longer be updated.",
        NotificationType.INFORMATION,
      ).addAction(
        NotificationAction.createExpiring("Disable the Build Server Procotol (BSP) plugin") { _, _ ->
          disablePlugins(project, listOf(bspPlugin))
        },
      ).notify(project)
  }

  private fun disablePlugins(project: Project, plugins: List<IdeaPluginDescriptor>) {
    val model = MyPluginModel(project)
    model.disable(plugins)
    model.apply(null)
  }
}
