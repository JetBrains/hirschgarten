
package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.ui.queryTab.registerBazelQueryToolWindow

class OpenBazelQueryToolWindowAction :
  SuspendableAction(
    BazelPluginBundle.message("action.open.bazelquery.toolwindow.text"),
    org.jetbrains.bazel.assets.BazelPluginIcons.bazelToolWindow,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    if (FeatureFlagsProvider.getFeatureFlags(project).isBazelQueryTabEnabled) {
      val toolWindowManager = ToolWindowManager.getInstance(project)
      val currentToolWindow = toolWindowManager.getToolWindow(BazelPluginConstants.BAZEL_QUERY_TOOLWINDOW_ID)

      withContext(Dispatchers.EDT) {
        if (currentToolWindow == null) {
          registerBazelQueryToolWindow(project)
        } else if (currentToolWindow.isVisible) {
          currentToolWindow.remove()
        } else {
          currentToolWindow.show()
        }
      }
    }
  }
}
