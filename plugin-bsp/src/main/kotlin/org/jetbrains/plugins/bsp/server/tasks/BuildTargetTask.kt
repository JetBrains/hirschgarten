package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

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

    val buildFuture = server.buildTargetCompile(compileParams)
    return BspTaskStatusLogger(buildFuture, bspBuildConsole, originId, cancelOn) { statusCode }.getResult()
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
      BspCoroutineService.getInstance(project).start {
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

public fun doesCompletableFutureGetThrowCancelledException(e: Exception): Boolean =
  (e is ExecutionException || e is InterruptedException) && e.cause is CancellationException
