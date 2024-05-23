package org.jetbrains.plugins.bsp.ui.console

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.rootDir

@Service(Service.Level.PROJECT)
public class BspConsoleService(project: Project) {
  public val bspBuildConsole: TaskConsole

  public val bspSyncConsole: TaskConsole

  init {
    val basePath = project.rootDir.path

    bspBuildConsole =
      BuildTaskConsole(project.getService(BuildViewManager::class.java), basePath, project.assets.presentableName)
    bspSyncConsole =
      SyncTaskConsole(project.getService(SyncViewManager::class.java), basePath, project.assets.presentableName)
  }

  public companion object {
    public fun getInstance(project: Project): BspConsoleService =
      project.getService(BspConsoleService::class.java)
  }
}
