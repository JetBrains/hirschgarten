package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.util.UUID

interface BuildTargetTask {
  suspend fun build(
    server: JoinedBuildServer,
    targetIds: List<Label>,
    buildConsole: TaskConsole,
    originId: String,
    debugFlags: List<String>,
  ): BazelStatus
}

suspend fun runBuildTargetTask(
  targetIds: List<Label>,
  project: Project,
  isDebug: Boolean,
  buildTargetTask: BuildTargetTask,
): BazelStatus {
  saveAllFiles()
  return withBackgroundProgress(project, BazelPluginBundle.message("background.progress.building.targets")) {
    // some languages require running `bazel build` with additional flags before debugging. e.g., python, c++
    // when this happens, isDebug should be set to true, and flags from "debug_flags" section of the project view file will be added
    val debugFlag = if (isDebug) project.connection.runWithServer { it.workspaceContext().debugFlags } else listOf()
    project.connection.runWithServer { server ->
      coroutineScope {
        val originId = "build-" + UUID.randomUUID().toString()
        val bspBuildConsole = ConsoleService.getInstance(project).buildConsole

        val taskListener = BazelBuildTaskListener(bspBuildConsole, originId)

        BazelTaskEventsService.getInstance(project).saveListener(originId, taskListener)

        val startBuildMessage = when (targetIds.size) {
          0 -> BazelPluginBundle.message("console.task.build.no.targets")
          1 -> BazelPluginBundle.message("console.task.build.in.progress.one", targetIds.first().toShortString(project))
          else -> BazelPluginBundle.message("console.task.build.in.progress.many", targetIds.size)
        }

        bspBuildConsole.startTask(
          taskId = originId,
          title = BazelPluginBundle.message("console.task.build.title"),
          message = startBuildMessage,
          cancelAction = { this@coroutineScope.cancel() },
          redoAction = {
            BazelCoroutineService.getInstance(project).start { runBuildTargetTask(targetIds, project, isDebug, buildTargetTask) }
          },
        )

        try {
          val buildDeferred = async { buildTargetTask.build(server, targetIds, bspBuildConsole, originId, debugFlag) }
          return@coroutineScope BspTaskStatusLogger(buildDeferred, bspBuildConsole, originId).getStatus()
        } finally {
          BazelTaskEventsService.getInstance(project).removeListener(originId)
        }
      }
    }
  }.also {
    VirtualFileManager.getInstance().asyncRefresh()
  }
}

object DefaultBuildTargetTask : BuildTargetTask {
  override suspend fun build(
    server: JoinedBuildServer,
    targetIds: List<Label>,
    buildConsole: TaskConsole,
    originId: String,
    debugFlags: List<String>,
  ): BazelStatus {
    val compileParams = CompileParams(targetIds, originId = originId, arguments = debugFlags + listOf("--keep_going"))
    return server.buildTargetCompile(compileParams).statusCode
  }
}

suspend fun runBuildTargetTask(
  targetIds: List<Label>,
  project: Project,
  isDebug: Boolean = false,
): BazelStatus = runBuildTargetTask(targetIds, project, isDebug, DefaultBuildTargetTask)
