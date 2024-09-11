package org.jetbrains.plugins.bsp.building

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.ui.console.BuildTaskConsole
import org.jetbrains.plugins.bsp.ui.console.SyncTaskConsole
import org.jetbrains.plugins.bsp.ui.console.TaskConsole

@Service(Service.Level.PROJECT)
public class BspConsoleService(project: Project) {
  public val bspBuildConsole: TaskConsole

  public val bspSyncConsole: TaskConsole

  init {
    val basePath = project.rootDir.path

    bspBuildConsole =
      BuildTaskConsole(project.getService(BuildViewManager::class.java), basePath, project.assets.presentableName, project)
    bspSyncConsole =
      SyncTaskConsole(project.getService(SyncViewManager::class.java), basePath, project.assets.presentableName, project)
  }

  public companion object {
    public fun getInstance(project: Project): BspConsoleService = project.getService(BspConsoleService::class.java)
  }
}

val Project.syncConsole: TaskConsole
  get() = BspConsoleService.getInstance(this).bspSyncConsole

suspend fun <T> TaskConsole.withSubtask(
  taskId: String,
  subtaskId: String,
  message: String,
  block: suspend (subtaskId: String) -> T,
): T {
  startSubtask(taskId, subtaskId, message)
  val result = block(subtaskId)
  finishSubtask(subtaskId, message)
  return result
}
