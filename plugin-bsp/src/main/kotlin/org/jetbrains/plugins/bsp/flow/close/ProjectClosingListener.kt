package org.jetbrains.plugins.bsp.flow.close

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.withModalProgress
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspCoroutineService

public class ProjectClosingListener : ProjectManagerListener {

  override fun projectClosing(project: Project) {
    if (project.isBspProject) {
      doProjectClosing(project)
    }
  }

  private fun doProjectClosing(project: Project) {
    BspCoroutineService.getInstance(project).start {
      withModalProgress(project, "Disconnecting...") {
        try {
          BspConnectionService.getInstance(project).value?.disconnect()
        } catch (e: Exception) {
          log.warn("One of the disconnect actions has failed!", e)
        }
      }
    }
  }

  private companion object {
    private val log = logger<ProjectClosingListener>()
  }
}
