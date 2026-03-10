package org.jetbrains.bazel.buildTask

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.toPromise
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskManager
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import com.intellij.task.impl.ProjectTaskList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.concurrency.Promise

internal class BazelProjectTaskRunner : ProjectTaskRunner() {
  override fun canRun(project: Project, projectTask: ProjectTask): Boolean =
    project.isBazelProject &&
    TrustedProjects.isProjectTrusted(project) &&
    canRun(projectTask)

  override fun canRun(projectTask: ProjectTask): Boolean =
    when (projectTask) {
      is ModuleBuildTask -> !projectTask.module.project.bazelJVMProjectSettings.enableBuildWithJps
      else -> false
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun run(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ): Promise<Result> = BazelCoroutineService.getInstance(project).startAsync {
    val targetsToBuild = obtainTargetsToBuild(project, tasks)
    val additionalTasks = AdditionalProjectTaskRunnerProvider.ep.extensionList.mapNotNull {
      it.createTask(project, projectTaskContext, targetsToBuild)
    }
    additionalTasks.forEach { it.preRun() }
    val result = runBuildTargetTask(
      targetsToBuild, project,
      customRedoAction = {
        // Rerun through ProjectTaskManager to make sure hotswap works
        ProjectTaskManager.getInstance(project).run(ProjectTaskList(tasks.toList()))
      },
    )
    additionalTasks.forEach { it.postRun(result) }
    result.toTaskRunnerResult()
  }.toPromise()

  private fun obtainTargetsToBuild(project: Project, tasks: Array<out ProjectTask>): List<Label> =
    tasks
      .filterIsInstance<ModuleBuildTask>()
      .mapNotNull { project.targetUtils.getBuildTargetForModule(it.module) }
      .filter { !it.noBuild }
      .map { it.id }

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
internal interface AdditionalProjectTaskRunnerProvider {
  fun createTask(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    targetsToBuild: List<Label>,
  ): AdditionalProjectTask?

  companion object {
    val ep: ExtensionPointName<AdditionalProjectTaskRunnerProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.additionalProjectTaskRunnerProvider")
  }
}

internal interface AdditionalProjectTask {
  suspend fun preRun()
  suspend fun postRun(result: BazelStatus)
}
