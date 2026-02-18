package org.jetbrains.bazel.ui.console

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jediterm.core.util.TermSize
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bsp.protocol.TaskId

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

  fun ptyTermSize(taskId: TaskId): TermSize? =
    buildConsole.ptyTermSize(taskId) ?: syncConsole.ptyTermSize(taskId)

  companion object {
    fun getInstance(project: Project): ConsoleService = project.getService(ConsoleService::class.java)
  }
}

val Project.syncConsole: TaskConsole
  get() = ConsoleService.getInstance(this).syncConsole

suspend fun <T> TaskConsole.withSubtask(
  subtaskId: TaskId,
  message: String,
  block: suspend (subtaskId: TaskId) -> T,
): T {
  startSubtask(subtaskId, message)
  try {
    val result = block(subtaskId)
    finishSubtask(subtaskId)
    return result
  } catch (ex: Throwable) {
    finishSubtask(subtaskId, result = FailureResultImpl(ex))
    throw ex
  }
}

suspend fun <T> Project.withSubtask(
  reporter: SequentialProgressReporter,
  subtaskId: TaskId,
  text: String,
  block: suspend (subtaskId: TaskId) -> T,
) {
  reporter.indeterminateStep(text) {
    syncConsole.withSubtask(
      subtaskId = subtaskId,
      message = text,
      block = block,
    )
  }
}
