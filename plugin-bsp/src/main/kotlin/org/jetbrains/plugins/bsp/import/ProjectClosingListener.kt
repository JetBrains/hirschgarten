package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.plugins.bsp.connection.BspConnectionService

public class ProjectClosingListener : ProjectManagerListener {

  override fun projectClosing(project: Project) {
    val bspConnectionService = BspConnectionService.getInstance(project)
    runModalTask("Disconnecting...", project = project, cancellable = false) {
      try {
        bspConnectionService.connection?.disconnect()
      } catch (e: Exception) {
        log.warn("One of the disconnect actions has failed!", e)
      }
    }
  }

  private companion object {
    private val log = logger<ProjectClosingListener>()
  }
}
