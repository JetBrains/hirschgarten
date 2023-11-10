package org.jetbrains.plugins.bsp.ui.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction

private val log = logger<DisconnectAction>()

public class DisconnectAction : SuspendableAction({ BspPluginBundle.message("disconnect.action.text") }), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    withBackgroundProgress(project, "Disconnecting...") {
      try {
        BspConnectionService.getInstance(project).value?.disconnect()
      } catch (e: Exception) {
        log.warn("One of the disconnect actions has failed!", e)
      }
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    val connection = BspConnectionService.getInstance(project).value
    e.presentation.isEnabled = connection?.isConnected() == true
  }
}
