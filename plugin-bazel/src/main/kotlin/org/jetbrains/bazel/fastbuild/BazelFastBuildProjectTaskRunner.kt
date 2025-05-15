package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.project.Project
import com.intellij.task.ModuleFilesBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.concurrency.Promise

class BazelFastBuildProjectTaskRunner : ProjectTaskRunner() {
  override fun canRun(projectTask: ProjectTask): Boolean = throw UnsupportedOperationException("Obsolete method")

  override fun canRun(
    project: Project,
    projectTask: ProjectTask,
    context: ProjectTaskContext?,
  ): Boolean =
    BazelFeatureFlags.fastBuildEnabled &&
      projectTask is ModuleFilesBuildTask &&
      !project.bazelJVMProjectSettings.enableBuildWithJps &&
      project.isBazelProject

  override fun run(
    project: Project,
    context: ProjectTaskContext,
    vararg tasks: ProjectTask?,
  ): Promise<Result> {
    // TODO: IntelliJ will try to hotswap using JPS and fail, displaying "Loaded classes are up to date. Nothing to reload."
    val moduleBuildTasks = tasks.filterIsInstance<ModuleFilesBuildTask>()
    return FastBuildUtils.fastBuildFilesPromise(project, moduleBuildTasks.flatMap { it.files.toList() })
  }
}
