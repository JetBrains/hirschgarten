package org.jetbrains.bazel.buildTask

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskManager
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import com.intellij.task.impl.ProjectTaskList
import com.intellij.workspaceModel.ide.legacyBridge.findModuleEntity
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.toPromiseWithoutLogError

internal class BazelProjectTaskRunner : ProjectTaskRunner() {
  // obsolete, kept for compatibility with 261.*
  @Suppress("removal", "OVERRIDE_DEPRECATION")
  override fun canRun(projectTask: ProjectTask): Boolean = false

  override fun canRun(project: Project, projectTask: ProjectTask, context: ProjectTaskContext?): Boolean =
    project.isBazelProject &&
    TrustedProjects.isProjectTrusted(project) &&
    projectTask is ModuleBuildTask

  override fun run(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ): Promise<Result> = BazelCoroutineService.getInstance(project).startAsync {
    val targetsToBuild = obtainTargetsToBuild(tasks)
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
  }.toPromiseWithoutLogError()

  private fun obtainTargetsToBuild(tasks: Array<out ProjectTask>): List<Label> =
    tasks.filterIsInstance<ModuleBuildTask>()
      .mapNotNull { it.module.findModuleEntity() }
      .mapNotNull { it.bazelModuleExtension?.label?.toLabel() }

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
@ApiStatus.Internal
interface AdditionalProjectTaskRunnerProvider {
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

@ApiStatus.Internal
interface AdditionalProjectTask {
  suspend fun preRun()
  suspend fun postRun(result: BazelStatus)
}
