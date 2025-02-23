package org.jetbrains.bazel.flow.open

import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import kotlin.io.path.isRegularFile

class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project
    if (project.bazelProjectSettings.projectViewPath?.isRegularFile() != true) {
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
