package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseHandler
import org.jetbrains.plugins.bsp.connection.BspConnectionService

public class BspProjectCloseHandler : ProjectCloseHandler {
  // TODO is is ta proper handler?
  override fun canClose(project: Project): Boolean {
    val bspConnectionService = BspConnectionService.getInstance(project)
    runBackgroundableTask("Disconnecting...") {
      try {
        bspConnectionService.connection!!.disconnect()
      } catch (ex: Exception) {
        // TODO implement scenario in which for some reason it is not possible to close resources
      }
    }

    return true
  }
}
