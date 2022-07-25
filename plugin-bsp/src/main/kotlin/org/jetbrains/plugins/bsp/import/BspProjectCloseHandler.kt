package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseHandler
import org.jetbrains.plugins.bsp.services.BspConnectionService

public class BspProjectCloseHandler: ProjectCloseHandler
{
  override fun canClose(project: Project): Boolean {
    val bspConnectionService = BspConnectionService.getInstance(project)
    try {
      bspConnectionService.disconnect()
    } catch (ex: Exception) {
      // TODO implement scenario in which for some reason it is not possible to close resources
    }
    return true
  }
}
