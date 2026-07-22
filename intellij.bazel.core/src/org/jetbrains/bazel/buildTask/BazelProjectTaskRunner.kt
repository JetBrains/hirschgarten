package org.jetbrains.bazel.buildTask

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleEntity
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
import org.jetbrains.bazel.workspacemodel.entities.targetKey
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.toPromiseWithoutLogError

internal class BazelProjectTaskRunner : ProjectTaskRunner() {
  override fun canRun(project: Project, projectTask: ProjectTask, context: ProjectTaskContext?): Boolean =
    project.isBazelProject &&
    TrustedProjects.isProjectTrusted(project) &&
    projectTask is ModuleBuildTask

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
  }.toPromiseWithoutLogError()

  private fun obtainTargetsToBuild(project: Project, tasks: Array<out ProjectTask>): List<Label> {
    val targetsToBuildProviders = TargetsToBuildProvider.ep.extensionList
    return tasks.filterIsInstance<ModuleBuildTask>()
      .mapNotNull { it.module.findModuleEntity() }
      .flatMap { moduleEntity -> targetsToBuildProviders.flatMap { it.getTargetsToBuild(project, moduleEntity) } }
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

@ApiStatus.Internal
interface TargetsToBuildProvider {
  fun getTargetsToBuild(project: Project, moduleEntity: ModuleEntity): List<Label>

  companion object {
    val ep: ExtensionPointName<TargetsToBuildProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.targetsToBuildProvider")
  }
}

internal class DefaultTargetsToBuildProvider : TargetsToBuildProvider {
  override fun getTargetsToBuild(project: Project, moduleEntity: ModuleEntity): List<Label> {
    return listOfNotNull(moduleEntity.bazelModuleExtension?.targetKey?.label)
  }
}
