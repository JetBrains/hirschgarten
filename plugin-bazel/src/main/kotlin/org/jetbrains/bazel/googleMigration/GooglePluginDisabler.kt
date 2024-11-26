package org.jetbrains.bazel.googleMigration

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier

class GooglePluginDisabler : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isBazelProject) return
    val googlePluginId = PluginId.getId("com.google.idea.bazel.ijwb")
    if (PluginManagerCore.isPluginInstalled(googlePluginId) && !PluginManagerCore.isDisabled(googlePluginId)) {
      BspBalloonNotifier.warn(
        title = "Warning: Conflicting Bazel Plugin Detected",
        content = "The Google Bazel plugin is currently enabled, which may cause conflicts with the JetBrains Bazel plugin.",
        action =
          NotificationAction.createSimpleExpiring(
            "Disable Google Bazel Plugin",
          ) {
            ApplicationManager.getApplication().invokeAndWait {
              PluginManagerCore.disablePlugin(googlePluginId)
            }
          },
      )
    }
  }
}
