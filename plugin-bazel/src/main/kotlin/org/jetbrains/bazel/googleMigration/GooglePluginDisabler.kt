package org.jetbrains.bazel.googleMigration

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.Messages

class GooglePluginDisabler : ProjectActivity {
  override suspend fun execute(project: Project) {
    ApplicationManager.getApplication().invokeLater {
      val conflictingPluginId = PluginId.getId("com.google.idea.bazel.ijwb")
      if (PluginManagerCore.isPluginInstalled(conflictingPluginId) && !PluginManagerCore.isDisabled(conflictingPluginId)) {
        PluginManagerCore.disablePlugin(conflictingPluginId)
        Messages.showWarningDialog(
          project,
          "The Google's Bazel plugin is enabled and may cause issues with the JetBrains Bazel plugin. It has been automatically disabled. Please restart the IDE to apply the changes.",
          "Warning: Conflicting Plugin Detected",
        )
      }
    }
  }
}
