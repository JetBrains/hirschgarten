package org.jetbrains.bazel.hotswap

import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import org.jetbrains.bazel.buildTask.AdditionalProjectTaskRunnerProvider
import org.jetbrains.bazel.config.isBspProject

class BazelHotSwapProjectTaskRunnerProvider : AdditionalProjectTaskRunnerProvider {
  override fun preRun(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ) {
    if (!project.isBspProject) return
    val hotSwappableDebugSession = BazelHotSwapManager.createHotSwappableDebugSession(project)
    hotSwappableDebugSession?.let { ClassFileManifestBuilder.initStateIfNotExists(it, project) }
  }

  override fun postRun(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ) {
    if (!project.isBspProject) return
    BazelHotSwapManager.reloadChangedClasses(project)
  }
}
