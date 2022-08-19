package org.jetbrains.plugins.bsp.services

import com.intellij.build.BuildViewManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.console.BspBuildConsole

public class BspBuildConsoleService(project: Project) {

  public val bspBuildConsole: BspBuildConsole =
    BspBuildConsole(project.getService(BuildViewManager::class.java), project.basePath!!)

  public companion object {
    public fun getInstance(project: Project): BspBuildConsoleService =
      project.getService(BspBuildConsoleService::class.java)
  }
}
