package org.jetbrains.bazel.hotswap

import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import org.jetbrains.bazel.buildTask.BspAdditionalProjectTaskRunnerProvider
import org.jetbrains.bazel.config.isBazelProject

class BazelHotSwapProjectTaskRunnerProvider : BspAdditionalProjectTaskRunnerProvider {
  override fun preRun(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ) {
    if (!project.isBazelProject) return
    val hotSwappableDebugSession = BazelHotSwapManager.createHotSwappableDebugSession(project)
    hotSwappableDebugSession?.let { ClassFileManifestBuilder.initStateIfNotExists(it, project) }
  }

  override fun postRun(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ) {
    if (!project.isBazelProject) return
    BazelHotSwapManager.reloadChangedClasses(project)
  }
}
