package org.jetbrains.bazel.ui.console

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.assets.assets
import org.jetbrains.bazel.config.rootDir

@Service(Service.Level.PROJECT)
class BspConsoleService(project: Project) {
  val bspBuildConsole: TaskConsole

  val bspSyncConsole: TaskConsole

  init {
    val basePath = project.rootDir.path

    bspBuildConsole =
      BuildTaskConsole(project.getService(BuildViewManager::class.java), basePath, project.assets.presentableName, project)
    bspSyncConsole =
      SyncTaskConsole(project.getService(SyncViewManager::class.java), basePath, project.assets.presentableName, project)
  }

  companion object {
    fun getInstance(project: Project): BspConsoleService = project.getService(BspConsoleService::class.java)
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
