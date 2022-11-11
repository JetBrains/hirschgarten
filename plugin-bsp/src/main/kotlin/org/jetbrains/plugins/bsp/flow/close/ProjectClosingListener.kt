package org.jetbrains.plugins.bsp.flow.close

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.plugins.bsp.config.BspProjectPropertiesService
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService

public class ProjectClosingListener : ProjectManagerListener {

  override fun projectClosing(project: Project) {
    val projectProperties = BspProjectPropertiesService.getInstance(project).value

    if (projectProperties.isBspProject) {
      doProjectClosing(project)
    }
  }

  private fun doProjectClosing(project: Project) {
    runModalTask("Disconnecting...", project = project, cancellable = false) {
      try {
        val connection = BspConnectionService.getInstance(project).value
        connection.disconnect()
      } catch (e: Exception) {
        log.warn("One of the disconnect actions has failed!", e)
      }
    }
  }

  private companion object {
    private val log = logger<ProjectClosingListener>()
  }
}
