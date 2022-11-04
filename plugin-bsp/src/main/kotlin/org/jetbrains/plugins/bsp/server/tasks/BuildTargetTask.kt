package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import java.util.*
import java.util.concurrent.CompletableFuture

public class BuildTargetTask(project: Project) : BspServerMultipleTargetsTask<CompileResult>(project) {

  public override fun execute(targetsIds: List<BuildTargetIdentifier>): CompileResult {
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
        bspBuildConsole.finishTask(buildId, "Failed", FailureResultImpl(exception))
      }
    }
}
