package org.jetbrains.bazel.ui.console

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir

@Service(Service.Level.PROJECT)
class ConsoleService(project: Project) {
  val buildConsole: TaskConsole

  val syncConsole: TaskConsole

  init {
    val basePath = project.rootDir.path

    buildConsole =
      BuildTaskConsole(
        project.getService(BuildViewManager::class.java),
        basePath,
        project,
      )
    syncConsole =
      SyncTaskConsole(
        project.getService(SyncViewManager::class.java),
        basePath,
        project,
      )
  }

  companion object {
    fun getInstance(project: Project): ConsoleService = project.getService(ConsoleService::class.java)
  }
}

val Project.syncConsole: TaskConsole
  get() = ConsoleService.getInstance(this).syncConsole

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
