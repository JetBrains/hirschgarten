package org.jetbrains.plugins.bsp.ui.console

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.openapi.project.Project

public class BspConsoleService(project: Project) {

  public val bspBuildConsole: TaskConsole =
    TaskConsole(project.getService(BuildViewManager::class.java), project.basePath!!)

  public val bspSyncConsole: TaskConsole =
    TaskConsole(project.getService(SyncViewManager::class.java), project.basePath!!)

  public val bspTestConsole: BspTargetTestConsole = BspTargetTestConsole()

  public val bspRunConsole: BspTargetRunConsole = BspTargetRunConsole()

  public companion object {
    public fun getInstance(project: Project): BspConsoleService =
      project.getService(BspConsoleService::class.java)
  }
}
