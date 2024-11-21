package org.jetbrains.bazel.flow.open

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectPreSyncHook

class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project
    if (project.bazelProjectSettings.projectViewPath == null) {
      project.bazelProjectSettings.withNewProjectViewPath(ProjectViewFileUtils.calculateProjectViewFilePath(project))
    }
    project.bazelProjectSettings.projectViewPath?.let { projectViewFilePath ->
      ProjectViewFileUtils.setProjectViewFileContentIfNotExists(projectViewFilePath, project)
    }
  }
}
