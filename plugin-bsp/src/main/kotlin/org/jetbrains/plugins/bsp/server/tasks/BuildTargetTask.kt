package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

public class BuildTargetTask(project: Project) : BspServerMultipleTargetsTask<CompileResult>("build targets", project) {

  protected override fun executeWithServer(server: BspServer, targetsIds: List<BuildTargetIdentifier>): CompileResult {
    val bspBuildConsole = BspConsoleService.getInstance(project).bspBuildConsole
    val originId = "build-" + UUID.randomUUID().toString()

    startBuildConsoleTask(targetsIds, bspBuildConsole, originId)
    val compileParams = createCompileParams(targetsIds, originId)

    return server.buildTargetCompile(compileParams).catchBuildErrors(bspBuildConsole, originId).get()
      .also { finishBuildConsoleTaskWithProperResult(it, bspBuildConsole, originId) }
  }

  private fun startBuildConsoleTask(
    targetIds: List<BuildTargetIdentifier>,
    bspBuildConsole: TaskConsole,
    originId: String
  ) {
    val startBuildMessage = calculateStartBuildMessage(targetIds)

    bspBuildConsole.startTask(originId, "Build", startBuildMessage)
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
    uuid: String
  ) = when (compileResult.statusCode) {
    StatusCode.OK -> bspBuildConsole.finishTask(uuid, "Successfully completed!")
    StatusCode.CANCELLED -> bspBuildConsole.finishTask(uuid, "Cancelled!")
    StatusCode.ERROR -> bspBuildConsole.finishTask(uuid, "Ended with an error!", FailureResultImpl())
    else -> bspBuildConsole.finishTask(uuid, "Finished!")
  }

  // TODO update and move
  private fun <T> CompletableFuture<T>.catchBuildErrors(
    bspBuildConsole: TaskConsole,
    buildId: String
  ): CompletableFuture<T> =
    this.whenComplete { _, exception ->
      exception?.let {
        if (isTimeoutException(it)) {
          val message = BspTasksBundle.message("task.timeout.message")
          bspBuildConsole.finishTask(buildId, "Timed out", FailureResultImpl(message))
        } else {
          bspBuildConsole.finishTask(buildId, "Failed", FailureResultImpl(it))
        }
      }
    }

  private fun isTimeoutException(e: Throwable): Boolean =
    e is CompletionException && e.cause is TimeoutException
}
