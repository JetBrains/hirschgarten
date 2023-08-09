package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
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

    bspBuildConsole.startTask(originId, "Build", startBuildMessage, {
      cancelOn.cancel(true)
    }) {
      BspCoroutineService.getInstance(project).startAsync {
        runBuildTargetTask(targetIds, project, log)
      }
    }
  }

  private fun calculateStartBuildMessage(targetIds: List<BuildTargetIdentifier>): String =
    when (targetIds.size) {
      0 -> "No targets to build! Skipping"
      1 -> "Building ${targetIds.first().uri}..."
      else -> "Building ${targetIds.size} targets..."
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
    StatusCode.OK -> bspBuildConsole.finishTask(uuid, "Successfully completed!")
    StatusCode.CANCELLED -> bspBuildConsole.finishTask(uuid, "Cancelled!")
    StatusCode.ERROR -> bspBuildConsole.finishTask(uuid, "Ended with an error!", FailureResultImpl())
    else -> bspBuildConsole.finishTask(uuid, "Finished!")
  }

  // TODO update and move
  private fun <T> CompletableFuture<T>.catchBuildErrors(
    bspBuildConsole: TaskConsole,
    buildId: String,
  ): CompletableFuture<T> =
    this.whenComplete { _, exception ->
      exception?.let {
        if (isTimeoutException(it)) {
          val message = BspTasksBundle.message("task.timeout.message")
          bspBuildConsole.finishTask(buildId, "Timed out", FailureResultImpl(message))
        } else if (isCancellationException(it)) {
          bspBuildConsole.finishTask(buildId, "Canceled", FailureResultImpl("Build task is canceled"))
        } else {
          bspBuildConsole.finishTask(buildId, "Failed", FailureResultImpl(it))
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

private fun doesCompletableFutureGetThrowCancelledException(e: Exception): Boolean =
  (e is ExecutionException || e is InterruptedException) && e.cause is CancellationException
