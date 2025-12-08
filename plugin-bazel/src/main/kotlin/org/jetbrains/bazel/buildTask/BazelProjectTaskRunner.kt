package org.jetbrains.bazel.buildTask

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.toPromise
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise

class BazelProjectTaskRunner : ProjectTaskRunner() {
  override fun canRun(project: Project, projectTask: ProjectTask): Boolean =
    project.isBazelProject &&
      project.isTrusted() &&
      canRun(projectTask)

  override fun canRun(projectTask: ProjectTask): Boolean =
    when (projectTask) {
      is ModuleBuildTask -> !projectTask.module.project.bazelJVMProjectSettings.enableBuildWithJps
      else -> false
    }

  override fun run(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ): Promise<Result> {
    val result = AsyncPromise<Result>()

    AdditionalProjectTaskRunnerProvider.ep.extensionList.forEach { it.preRun(project, projectTaskContext, *tasks) }

    val res = runModuleBuildTasks(project, tasks.filterIsInstance<ModuleBuildTask>())
    res.then {
      AdditionalProjectTaskRunnerProvider.ep.extensionList.forEach { it.postRun(project, projectTaskContext, *tasks) }
      result.setResult(it)
    }

    return result
  }

  private fun runModuleBuildTasks(project: Project, tasks: List<ModuleBuildTask>): Promise<Result> {
    val targetsToBuild = obtainTargetsToBuild(project, tasks)
    return buildBspTargets(project, targetsToBuild)
  }

  private fun obtainTargetsToBuild(project: Project, tasks: List<ModuleBuildTask>): List<BuildTarget> {
    val targetUtils = project.targetUtils
    return tasks.mapNotNull { targetUtils.getBuildTargetForModule(it.module) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun buildBspTargets(project: Project, targetsToBuild: List<BuildTarget>): Promise<Result> {
    val targetIdentifiers = targetsToBuild.filter { !it.noBuild }.map { it.id }
    val result =
      BazelCoroutineService.getInstance(project).startAsync {
        runBuildTargetTask(targetIdentifiers, project)
      }
    return result
      .toPromise()
      .then { it?.toTaskRunnerResult() ?: TaskRunnerResults.FAILURE }
  }

  private fun BazelStatus.toTaskRunnerResult() =
    when (this) {
      BazelStatus.SUCCESS -> TaskRunnerResults.SUCCESS
      BazelStatus.CANCEL -> TaskRunnerResults.ABORTED
      else -> TaskRunnerResults.FAILURE
    }
}

/**
 * This extension is useful when there is a need to inject before and after actions with respect to the project task runner
 */
interface AdditionalProjectTaskRunnerProvider {
  fun preRun(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  )

  fun postRun(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  )

  companion object {
    val ep: ExtensionPointName<AdditionalProjectTaskRunnerProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.additionalProjectTaskRunnerProvider")
  }
}
