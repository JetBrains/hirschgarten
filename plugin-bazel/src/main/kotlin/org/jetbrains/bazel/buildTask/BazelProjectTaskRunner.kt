package org.jetbrains.bazel.buildTask

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.bazel.build.session.BazelBuildRunner
import org.jetbrains.bazel.label.Label

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
    val labelsToBuild = obtainTargetsToBuild(project, tasks)
    return buildTargets(project, labelsToBuild)
  }

  private fun obtainTargetsToBuild(project: Project, tasks: List<ModuleBuildTask>): List<Label> {
    val targetUtils = project.targetUtils
    return tasks.mapNotNull { task -> targetUtils.getBuildTargetForModule(task.module)?.id }
  }

  private fun buildTargets(project: Project, targetsToBuild: List<Label>): Promise<Result> {
    val result = AsyncPromise<Result>()
    val filtered = targetsToBuild.distinct()
    if (filtered.isEmpty()) {
      result.setResult(TaskRunnerResults.SUCCESS)
      return result
    }

    BazelBuildRunner(project).build(filtered) { exitCode ->
      val status = if (exitCode == 0) TaskRunnerResults.SUCCESS else TaskRunnerResults.FAILURE
      result.setResult(status)
    }
    return result
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
