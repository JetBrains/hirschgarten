package org.jetbrains.plugins.bsp.runConfig.building

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import com.intellij.util.ModalityUiUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.plugins.bsp.server.tasks.BuildTargetTask
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public class BspBuildTaskRunner : ProjectTaskRunner() {
  override fun canRun(projectTask: ProjectTask): Boolean {
    return true
  }

  // It will be run in a separate thread
  private fun buildAllBspTargets(project: Project, promise: AsyncPromise<Result>) {
    val bspBuildConsole = BspConsoleService.getInstance(project).bspBuildConsole
    bspBuildConsole.startTask("bsp-build", "Build", "Building...")

    val magicMetaModel = MagicMetaModelService.getInstance(project).value

    val targets: List<BuildTarget> = magicMetaModel.getAllLoadedTargets() + magicMetaModel.getAllNotLoadedTargets()

    val buildCompileResult: CompileResult = BuildTargetTask(project).execute(
      targets
        .filter { it.capabilities.canCompile }
        .map { it.id }
    )
    return if (buildCompileResult.statusCode == StatusCode.OK) {
      bspBuildConsole.finishTask("bsp-build", "Build done!", SuccessResultImpl())
      promise.setResult(TaskRunnerResults.SUCCESS)
    } else {
      bspBuildConsole.finishTask("bsp-build", "Build failed!", FailureResultImpl())
      promise.setResult(TaskRunnerResults.FAILURE)
    }
  }

  override fun run(project: Project, context: ProjectTaskContext, vararg tasks: ProjectTask?): Promise<Result> {
    val promise = AsyncPromise<Result>()
    ModalityUiUtil.invokeLaterIfNeeded(
      ModalityState.defaultModalityState(), project.disposed
    ) { buildAllBspTargets(project, promise) }
    return promise
  }
}
