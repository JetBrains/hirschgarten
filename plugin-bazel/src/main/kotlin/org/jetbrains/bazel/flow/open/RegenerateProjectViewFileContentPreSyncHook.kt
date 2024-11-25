package org.jetbrains.bazel.flow.open

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectPreSyncHook
import kotlin.io.path.isRegularFile

class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project
    if (project.bazelProjectSettings.projectViewPath?.isRegularFile() == false) {
      val projectViewFilePath =
        ProjectViewFileUtils.calculateProjectViewFilePath(
          project = project,
          generateContent = true,
          overwrite = false,
          bazelPackageDir = null,
        )
      project.bazelProjectSettings = project.bazelProjectSettings.withNewProjectViewPath(projectViewFilePath)
    }
  }
}
