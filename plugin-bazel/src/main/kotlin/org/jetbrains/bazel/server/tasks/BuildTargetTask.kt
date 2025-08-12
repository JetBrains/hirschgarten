package org.jetbrains.bazel.server.tasks

import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.TaskId
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileReport
import org.jetbrains.bsp.protocol.CompileResult
import java.nio.file.Path
import java.util.UUID

@InternalApi
class BuildTargetTask(private val project: Project) {
  suspend fun execute(targetsIds: List<Label>, arguments: List<String>): CompileResult =
    coroutineScope {
      val bspBuildConsole = ConsoleService.getInstance(project).buildConsole
      val originId = "build-" + UUID.randomUUID().toString()

      val taskListener =
        object : BazelTaskListener {
          override fun onTaskStart(
            taskId: TaskId,
            parentId: TaskId?,
            message: String,
            data: Any?,
          ) {
            if (parentId == null) {
              bspBuildConsole.startSubtask(originId, taskId, message)
            } else {
              bspBuildConsole.startSubtask(taskId, parentId, message)
            }
          }

          override fun onTaskProgress(
            taskId: TaskId,
            message: String,
            data: Any?,
          ) {
            bspBuildConsole.addMessage(taskId, message)
          }

          override fun onTaskFinish(
            taskId: TaskId,
            parentId: TaskId?,
            message: String,
            status: BazelStatus,
            data: Any?,
          ) {
            when (data) {
              is CompileReport -> {
                if (data.errors == 0 && status == BazelStatus.SUCCESS) {
                  bspBuildConsole.finishSubtask(taskId, message, FailureResultImpl())
                } else if (status == BazelStatus.CANCEL) {
                  bspBuildConsole.finishSubtask(taskId, message, SkippedResultImpl())
                } else {
                  bspBuildConsole.finishSubtask(taskId, message, SuccessResultImpl())
                }
              }

              else -> bspBuildConsole.finishSubtask(taskId, message, SuccessResultImpl())
            }
          }

          override fun onDiagnostic(
            textDocument: Path?,
            buildTarget: Label,
            line: Int,
            character: Int,
            severity: MessageEvent.Kind,
            message: String,
          ) {
            bspBuildConsole.addDiagnosticMessage(
              originId,
              textDocument,
              line,
              character,
              message,
              severity,
            )
          }

          override fun onLogMessage(message: String) {
            bspBuildConsole.addMessage(originId, message)
          }
        }

      BazelTaskEventsService.getInstance(project).saveListener(originId, taskListener)

      startBuildConsoleTask(targetsIds, bspBuildConsole, originId, this)
      val compileParams = CompileParams(targetsIds, originId = originId, arguments = arguments + listOf("--keep_going"))
      try {
        val buildDeferred =
          async {
            BazelWorkspaceResolveService
              .getInstance(project)
              .withEndpointProxy { it.buildTargetCompile(compileParams) }
          }
        return@coroutineScope BspTaskStatusLogger(buildDeferred, bspBuildConsole, originId) { statusCode }.getResult()
      } finally {
        BazelTaskEventsService.getInstance(project).removeListener(originId)
      }
    }

  private fun startBuildConsoleTask(
    targetIds: List<Label>,
    bspBuildConsole: TaskConsole,
    originId: String,
    cs: CoroutineScope,
  ) {
    val startBuildMessage = calculateStartBuildMessage(targetIds)

    bspBuildConsole.startTask(
      taskId = originId,
      title = BazelPluginBundle.message("console.task.build.title"),
      message = startBuildMessage,
      cancelAction = { cs.cancel() },
      redoAction = { BazelCoroutineService.getInstance(project).start { runBuildTargetTask(targetIds, project) } },
    )
  }

  private fun calculateStartBuildMessage(targetIds: List<Label>): String =
    when (targetIds.size) {
      0 -> BazelPluginBundle.message("console.task.build.no.targets")
      1 -> BazelPluginBundle.message("console.task.build.in.progress.one", targetIds.first().toShortString(project))
      else -> BazelPluginBundle.message("console.task.build.in.progress.many", targetIds.size)
    }
}

suspend fun runBuildTargetTask(
  targetIds: List<Label>,
  project: Project,
  isDebug: Boolean = false,
): CompileResult? {
  saveAllFiles()
  return withBackgroundProgress(project, BazelPluginBundle.message("background.progress.building.targets")) {
    // some languages require running `bazel build` with additional flags before debugging. e.g., python, c++
    // when this happens, isDebug should be set to true, and flags from "debug_flags" section of the project view file will be added
    val debugFlag = if (isDebug) project.connection.runWithServer { it.workspaceContext().debugFlags.values } else listOf()
    project.connection.runWithServer { BuildTargetTask(project).execute(targetIds, debugFlag) }
  }.also {
    VirtualFileManager.getInstance().asyncRefresh()
  }
}
