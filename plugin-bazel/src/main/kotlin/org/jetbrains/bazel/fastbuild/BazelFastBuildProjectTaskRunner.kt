package org.jetbrains.bazel.fastbuild

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ModuleFilesBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise

class BazelFastBuildProjectTaskRunner: ProjectTaskRunner() {
  override fun canRun(projectTask: ProjectTask): Boolean {
    return when (projectTask) {
      is ModuleFilesBuildTask -> !projectTask.module.project.bazelProjectSettings.enableBuildWithJps
      else -> false
    }
  }

  override fun canRun(
    project: Project,
    projectTask: ProjectTask,
    context: ProjectTaskContext?
  ): Boolean {
    return Registry.`is`(FastBuildUtils.fastBuildEnabledKey) &&
      project.isBazelProject &&
      project.isTrusted() &&
      canRun(projectTask)
  }

  override fun run(
    project: Project,
    context: ProjectTaskContext,
    vararg tasks: ProjectTask?
  ): Promise<Result> {
    val moduleBuildTasks = tasks.filterIsInstance<ModuleFilesBuildTask>()
    return FastBuildUtils.fastBuildFiles(project, moduleBuildTasks.flatMap { it.files.toList() })
  }
}
