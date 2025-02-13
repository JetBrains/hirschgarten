package org.jetbrains.plugins.bsp.impl.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileReport
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.action.saveAllFiles
import org.jetbrains.plugins.bsp.annotations.PublicApi
import org.jetbrains.plugins.bsp.building.BspConsoleService
import org.jetbrains.plugins.bsp.building.TaskConsole
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.taskEvents.BspTaskEventsService
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.TaskId
import java.util.UUID
import java.util.concurrent.CancellationException

@ApiStatus.Internal
public class BuildTargetTask(project: Project) : BspServerMultipleTargetsTask<CompileResult>("build targets", project) {
  private val log = logger<BuildTargetTask>()

  protected override suspend fun executeWithServer(
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    targetsIds: List<BuildTargetIdentifier>,
  ): CompileResult =
    coroutineScope {
      val bspBuildConsole = BspConsoleService.getInstance(project).bspBuildConsole
      val originId = "build-" + UUID.randomUUID().toString()

      val taskListener =
        object : BspTaskListener {
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
            status: StatusCode,
            data: Any?,
          ) {
            when (data) {
              is CompileReport -> {
                if (data.errors > 0 || status == StatusCode.ERROR) {
                  bspBuildConsole.finishSubtask(taskId, message, FailureResultImpl())
                } else if (status == StatusCode.CANCELLED) {
                  bspBuildConsole.finishSubtask(taskId, message, SkippedResultImpl())
                } else {
                  bspBuildConsole.finishSubtask(taskId, message, SuccessResultImpl())
                }
              }

              else -> bspBuildConsole.finishSubtask(taskId, message, SuccessResultImpl())
            }
          }

          override fun onDiagnostic(
            textDocument: String,
            buildTarget: String,
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

      BspTaskEventsService.getInstance(project).saveListener(originId, taskListener)

      startBuildConsoleTask(targetsIds, bspBuildConsole, originId, this)
      val compileParams = createCompileParams(targetsIds, originId)

      try {
        val buildDeferred = async { server.buildTargetCompile(compileParams).await() }
        return@coroutineScope BspTaskStatusLogger(buildDeferred, bspBuildConsole, originId) { statusCode }.getResult()
      } finally {
        BspTaskEventsService.getInstance(project).removeListener(originId)
      }
    }

  private fun startBuildConsoleTask(
    targetIds: List<BuildTargetIdentifier>,
    bspBuildConsole: TaskConsole,
    originId: String,
    cs: CoroutineScope,
  ) {
    val startBuildMessage = calculateStartBuildMessage(targetIds)

    bspBuildConsole.startTask(
      taskId = originId,
      title = BspPluginBundle.message("console.task.build.title"),
      message = startBuildMessage,
      cancelAction = { cs.cancel() },
      redoAction = { BspCoroutineService.getInstance(project).start { runBuildTargetTask(targetIds, project, log) } },
    )
  }

  private fun calculateStartBuildMessage(targetIds: List<BuildTargetIdentifier>): String =
    when (targetIds.size) {
      0 -> BspPluginBundle.message("console.task.build.no.targets")
      1 -> BspPluginBundle.message("console.task.build.in.progress.one", targetIds.first().uri)
      else -> BspPluginBundle.message("console.task.build.in.progress.many", targetIds.size)
    }

  private fun createCompileParams(targetIds: List<BuildTargetIdentifier>, originId: String) =
    CompileParams(targetIds).apply {
      this.originId = originId
      this.arguments = listOf("--keep_going")
    }
}

@PublicApi
public suspend fun runBuildTargetTask(
  targetIds: List<BuildTargetIdentifier>,
  project: Project,
  log: Logger,
): CompileResult? =
  try {
    saveAllFiles()
    withBackgroundProgress(project, "Building target(s)...") {
      BuildTargetTask(project).connectAndExecute(targetIds)
    }.also {
      VirtualFileManager.getInstance().asyncRefresh()
    }
  } catch (e: Exception) {
    when {
      e is CancellationException -> CompileResult(StatusCode.CANCELLED)

      else -> {
        log.error(e)
        null
      }
    }
  }
