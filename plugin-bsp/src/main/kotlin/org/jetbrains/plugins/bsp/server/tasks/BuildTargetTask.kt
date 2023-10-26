package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

public class BuildTargetTask(project: Project) : BspServerMultipleTargetsTask<CompileResult>("build targets", project) {
  private val log = logger<BuildTargetTask>()

  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetsIds: List<BuildTargetIdentifier>,
  ): CompileResult {
    val bspBuildConsole = BspConsoleService.getInstance(project).bspBuildConsole
    val originId = "build-" + UUID.randomUUID().toString()
    val cancelOn = CompletableFuture<Void>()

    startBuildConsoleTask(targetsIds, bspBuildConsole, originId, cancelOn)
    val compileParams = createCompileParams(targetsIds, originId)

    return server
      .buildTargetCompile(compileParams)
      .reactToExceptionIn(cancelOn)
      .catchBuildErrors(bspBuildConsole, originId)
      .get()
      .also { finishBuildConsoleTaskWithProperResult(it, bspBuildConsole, originId) }
  }

  private fun startBuildConsoleTask(
    targetIds: List<BuildTargetIdentifier>,
    bspBuildConsole: TaskConsole,
    originId: String,
    cancelOn: CompletableFuture<Void>,
  ) {
    val startBuildMessage = calculateStartBuildMessage(targetIds)

    bspBuildConsole.startTask(originId, BspPluginBundle.message("console.task.build.title"), startBuildMessage, {
      cancelOn.cancel(true)
    }) {
      BspCoroutineService.getInstance(project).startAsync {
        runBuildTargetTask(targetIds, project, log)
      }
    }
  }

  private fun calculateStartBuildMessage(targetIds: List<BuildTargetIdentifier>): String =
    when (targetIds.size) {
      0 -> BspPluginBundle.message("console.task.build.no.targets")
      1 -> BspPluginBundle.message("console.task.build.in.progress.one", targetIds.first().uri)
      else -> BspPluginBundle.message("console.task.build.in.progress.many", targetIds.size)
    }

  private fun createCompileParams(targetIds: List<BuildTargetIdentifier>, originId: String) =
    CompileParams(targetIds)
      .apply {
        this.originId = originId
      }

  private fun finishBuildConsoleTaskWithProperResult(
    compileResult: CompileResult,
    bspBuildConsole: TaskConsole,
    uuid: String,
  ) = when (compileResult.statusCode) {
    StatusCode.OK -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.ok"))
    StatusCode.CANCELLED -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.cancelled"))
    StatusCode.ERROR -> bspBuildConsole.finishTask(uuid,
      BspPluginBundle.message("console.task.status.error"), FailureResultImpl())
    else -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.other"))
  }

  // TODO update and move
  private fun <T> CompletableFuture<T>.catchBuildErrors(
    bspBuildConsole: TaskConsole,
    buildId: String,
  ): CompletableFuture<T> =
    this.whenComplete { _, exception ->
      exception?.let {
        if (isTimeoutException(it)) {
          val message = BspPluginBundle.message("console.task.exception.timeout.message")
          bspBuildConsole.finishTask(buildId,
            BspPluginBundle.message("console.task.exception.timed.out"), FailureResultImpl(message))
        } else if (isCancellationException(it)) {
          bspBuildConsole.finishTask(buildId, BspPluginBundle.message("console.task.exception.cancellation"),
            FailureResultImpl(BspPluginBundle.message("console.task.exception.cancellation.message")))
        } else {
          bspBuildConsole.finishTask(buildId,
            BspPluginBundle.message("console.task.exception.other"), FailureResultImpl(it))
        }
      }
    }

  private fun isTimeoutException(e: Throwable): Boolean =
    e is CompletionException && e.cause is TimeoutException

  private fun isCancellationException(e: Throwable): Boolean =
    e is CompletionException && e.cause is CancellationException
}

public suspend fun runBuildTargetTask(
  targetIds: List<BuildTargetIdentifier>,
  project: Project,
  log: Logger,
): CompileResult? =
  try {
    saveAllFiles()
    withBackgroundProgress(project, "Building target(s)...") {
      BuildTargetTask(project).connectAndExecute(targetIds)
    }
  } catch (e: Exception) {
    when {
      doesCompletableFutureGetThrowCancelledException(e) ->
        CompileResult(StatusCode.CANCELLED)

      else -> {
        log.error(e)
        null
      }
    }
  }

// TODO https://youtrack.jetbrains.com/issue/BAZEL-630
public fun saveAllFiles() {
  ApplicationManager.getApplication().invokeAndWait {
    FileDocumentManager.getInstance().saveAllDocuments()
  }
}

private fun doesCompletableFutureGetThrowCancelledException(e: Exception): Boolean =
  (e is ExecutionException || e is InterruptedException) && e.cause is CancellationException
