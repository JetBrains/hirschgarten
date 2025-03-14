package org.jetbrains.bazel.flow.open.sync

import org.jetbrains.bazel.buildTask.buildProject
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectPostSyncHook

class JpsBuildPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    if (!project.bazelProjectSettings.enableBuildWithJps) return
    project.buildProject()
  }
}
