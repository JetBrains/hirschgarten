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
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import java.util.UUID
import kotlin.random.Random

interface BuildTargetTask {
  suspend fun build(
    server: JoinedBuildServer,
    targetIds: List<Label>,
    buildConsole: TaskConsole,
    taskId: TaskId,
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
        val taskGroupId = TaskGroupId("build-" + Random.nextBytes(8).toHexString())
        val bspBuildConsole = ConsoleService.getInstance(project).buildConsole

        val taskListener = BazelBuildTaskListener(bspBuildConsole)
        BazelTaskEventsService.getInstance(project).saveListener(taskGroupId, taskListener)

        try {
          val taskId = taskGroupId.task("build-${project.name}-${Random.nextBytes(8).toHexString()}")
          bspBuildConsole.startTask(
            taskId = taskId,
            title = BazelPluginBundle.message("console.task.build.title"),
            message = when (targetIds.size) {
              0 -> BazelPluginBundle.message("console.task.build.no.targets")
              1 -> BazelPluginBundle.message("console.task.build.in.progress.one", targetIds.first().toShortString(project))
              else -> BazelPluginBundle.message("console.task.build.in.progress.many", targetIds.size)
            },
            cancelAction = { this@coroutineScope.cancel() },
            redoAction = {
              BazelCoroutineService.getInstance(project).start { runBuildTargetTask(targetIds, project, isDebug, buildTargetTask) }
            },
          )

          val buildDeferred = async { buildTargetTask.build(server, targetIds, bspBuildConsole, taskId, debugFlag) }
          return@coroutineScope BspTaskStatusLogger(buildDeferred, bspBuildConsole, taskId).getStatus()
        } finally {
          BazelTaskEventsService.getInstance(project).removeListener(taskGroupId)
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
    taskId: TaskId,
    debugFlags: List<String>,
  ): BazelStatus {
    val compileParams = CompileParams(taskId,targetIds, arguments = debugFlags + listOf("--keep_going"))
    return server.buildTargetCompile(compileParams).statusCode
  }
}

suspend fun runBuildTargetTask(
  targetIds: List<Label>,
  project: Project,
  isDebug: Boolean = false,
): BazelStatus = runBuildTargetTask(targetIds, project, isDebug, DefaultBuildTargetTask)
