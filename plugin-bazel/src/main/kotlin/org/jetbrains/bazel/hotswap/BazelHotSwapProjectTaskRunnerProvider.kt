package org.jetbrains.bazel.hotswap

import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.buildTask.BspAdditionalProjectTaskRunnerProvider

class BazelHotSwapProjectTaskRunnerProvider : BspAdditionalProjectTaskRunnerProvider {
  override fun preRun(
    project: Project,
    projectTaskContext: ProjectTaskContext,
    vararg tasks: ProjectTask,
  ) {
    val hotSwappableDebugSession = BazelHotSwapManager.createHotSwappableDebugSession(project)
    hotSwappableDebugSession?.env?.let { ClassFileManifestBuilder.initStateIfNotExists(it) }
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
