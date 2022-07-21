package org.jetbrains.plugins.bsp.services

import com.intellij.build.SyncViewManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.console.BspSyncConsole

public class BspSyncConsoleService(project: Project) {

  public val bspSyncConsole: BspSyncConsole =
    BspSyncConsole(project.getService(SyncViewManager::class.java), project.basePath!!)

  public companion object {
    public fun getInstance(project: Project): BspSyncConsoleService =
      project.getService(BspSyncConsoleService::class.java)
  }
}