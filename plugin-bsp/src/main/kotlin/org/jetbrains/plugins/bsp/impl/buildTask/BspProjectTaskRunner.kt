package org.jetbrains.plugins.bsp.buildTask

import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.toPromise
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.server.tasks.runBuildTargetTask
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

public class BspProjectTaskRunner : ProjectTaskRunner() {
  private val log = logger<BspProjectTaskRunner>()

  override fun canRun(project: Project, projectTask: ProjectTask): Boolean =
    project.isBspProject &&
      project.isTrusted() &&
      canRun(projectTask)

  override fun canRun(projectTask: ProjectTask): Boolean =
    when (projectTask) {
      is JpsOnlyModuleBuildTask -> false
      is BspOnlyModuleBuildTask -> true
      is ModuleBuildTask -> !JpsFeatureFlags.isJpsCompilationAsDefaultEnabled
      else -> false
    }

  override fun run(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ): Promise<Result> {
    val result = AsyncPromise<Result>()

    val res = runModuleBuildTasks(project, tasks.filterIsInstance<ModuleBuildTask>())
    res.then { result.setResult(it) }

    return result
  }

  private fun runModuleBuildTasks(project: Project, tasks: List<ModuleBuildTask>): Promise<Result> {
    val targetsToBuild = obtainTargetsToBuild(project, tasks)
    return buildBspTargets(project, targetsToBuild)
  }

  private fun obtainTargetsToBuild(project: Project, tasks: List<ModuleBuildTask>): List<BuildTargetInfo> {
    val temporaryTargetUtils = project.temporaryTargetUtils
    return tasks.mapNotNull { temporaryTargetUtils.getBuildTargetInfoForModule(it.module) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun buildBspTargets(project: Project, targetsToBuild: List<BuildTargetInfo>): Promise<Result> {
    val targetIdentifiers = targetsToBuild.filter { it.capabilities.canCompile }.map { it.id }
    val result =
      BspCoroutineService.getInstance(project).startAsync {
        runBuildTargetTask(targetIdentifiers, project, log)
      }
    return result
      .toPromise()
      .then { it?.toTaskRunnerResult() ?: TaskRunnerResults.FAILURE }
  }

  private fun CompileResult.toTaskRunnerResult() =
    when (statusCode) {
      StatusCode.OK -> TaskRunnerResults.SUCCESS
      StatusCode.CANCELLED -> TaskRunnerResults.ABORTED
      else -> TaskRunnerResults.FAILURE
    }
}
