package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.fus.BazelBuildInvocationCollector
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.debugFlags
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.ShowConsole
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import kotlin.random.Random

@ApiStatus.Internal
interface BuildTargetTask {
  suspend fun build(
    server: BazelServerFacade,
    targetIds: List<Label>,
    buildConsole: TaskConsole,
    taskId: TaskId,
    debugFlags: List<String>,
  ): BazelStatus
}

@ApiStatus.Internal
suspend fun runBuildTargetTask(
  targetIds: List<Label>,
  project: Project,
  isDebug: Boolean,
  buildTargetTask: BuildTargetTask,
  customRedoAction: (suspend () -> Unit)? = null,
): BazelStatus {
  BazelBuildInvocationCollector.logStarted(project, targetIds.size)
  try {
    saveAllFiles()
    return withBackgroundProgress(project, BazelPluginBundle.message("background.progress.building.targets")) {
      // some languages require running `bazel build` with additional flags before debugging. e.g., python, c++
      // when this happens, isDebug should be set to true, and flags from "debug_flags" section of the project view file will be added
      val debugFlags = if (isDebug) project.connection.runWithServer { it.projectView.debugFlags } else listOf()
      val taskGroupId = TaskGroupId("build-" + Random.nextBytes(8).toHexString())
      val taskId = taskGroupId.task("build-${project.name}-${Random.nextBytes(8).toHexString()}")
      project.connection.runWithServer(taskId) { server ->
        val buildConsole = ConsoleService.getInstance(project).buildConsole
        BazelTaskEventsService.getInstance(project).saveListener(taskGroupId, BazelBuildTaskListener(buildConsole))
        val status = try {
          val redoAction = customRedoAction ?: {
            runBuildTargetTask(targetIds, project, isDebug, buildTargetTask)
            Unit
          }
          runBuildConsoleTask(project, taskId, targetIds, redoAction = redoAction) {
            buildTargetTask.build(server, targetIds, buildConsole, taskId, debugFlags)
          }
        }
        finally {
          BazelTaskEventsService.getInstance(project).removeListener(taskGroupId)
        }
        // asyncRefresh() refreshes the whole VFS, which includes all projects that have ever been opened in the IDE, not just the current one.
        // That can lead to long UI freezes, which is why asyncRefresh() is now deprecated in IJ platform.
        // On the other hand, BazelOutFileHardLinks handles VFS refresh granularly:
        // only on project sync, and only for changed Bazel outputs that are used in the current project.
        if (!server.outFileHardLinks.allHardLinksCreatedSuccessfully) {
          VirtualFileManager.getInstance().asyncRefresh()
        }
        status
      }
    }
  }
  finally {
    BazelBuildInvocationCollector.logFinished(project)
  }
}

/**
 * Shows [build] as a task of [taskId] in the build console, and finishes the task with the status that [build] returns.
 * [build] must run Bazel with [taskId], so that the diagnostics of the build go to this task.
 */
internal suspend fun runBuildConsoleTask(
  project: Project,
  taskId: TaskId,
  targetIds: List<Label>,
  showConsole: ShowConsole = ShowConsole.ALWAYS,
  redoAction: (suspend () -> Unit)? = null,
  build: suspend () -> BazelStatus,
): BazelStatus = coroutineScope {
  val buildConsole = ConsoleService.getInstance(project).buildConsole
  buildConsole.startTask(
    taskId = taskId,
    title = BazelPluginBundle.message("console.task.build.title"),
    message = when (targetIds.size) {
      0 -> BazelPluginBundle.message("console.task.build.no.targets")
      1 -> BazelPluginBundle.message("console.task.build.in.progress.one", targetIds.first().toShortString(project))
      else -> BazelPluginBundle.message("console.task.build.in.progress.many", targetIds.size)
    },
    cancelAction = { this@coroutineScope.cancel() },
    redoAction = redoAction,
    showConsole = showConsole,
  )
  BspTaskStatusLogger(async { build() }, buildConsole, taskId).getStatus()
}

@ApiStatus.Internal
object DefaultBuildTargetTask : BuildTargetTask {
  override suspend fun build(
    server: BazelServerFacade,
    targetIds: List<Label>,
    buildConsole: TaskConsole,
    taskId: TaskId,
    debugFlags: List<String>,
  ): BazelStatus {
    val compileParams = CompileParams(taskId, targetIds, arguments = debugFlags + listOf("--keep_going"))
    return server.buildTargetCompile(compileParams).statusCode
  }
}

@ApiStatus.Internal
suspend fun runBuildTargetTask(
  targetIds: List<Label>,
  project: Project,
  isDebug: Boolean = false,
  customRedoAction: (suspend () -> Unit)? = null,
): BazelStatus = runBuildTargetTask(targetIds, project, isDebug, DefaultBuildTargetTask, customRedoAction)
