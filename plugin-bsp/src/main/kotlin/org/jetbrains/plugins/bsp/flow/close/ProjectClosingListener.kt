package org.jetbrains.plugins.bsp.flow.close

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService

public class ProjectClosingListener : ProjectManagerListener {
  override fun projectClosing(project: Project) {
    if (project.isBspProject) {
      doProjectClosing(project)
    }
  }

  private fun doProjectClosing(project: Project) {
    runWithModalProgressBlocking(project, BspPluginBundle.message("progress.bar.project.close.disconnect")) {
      try {
        BspConnectionService.getInstance(project).value?.disconnect()
      } catch (e: Exception) {
        log.warn("One of the disconnect actions has failed!", e)
      }
    }
  }

  private companion object {
    private val log = logger<ProjectClosingListener>()
  }
}
