package org.jetbrains.bazel.flow.open

import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.settings.bazel.openProjectViewInEditor
import org.jetbrains.bazel.settings.bazel.setProjectViewPath
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import kotlin.io.path.isRegularFile

class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project

    val projectViewPath = project.bazelProjectSettings.projectViewPath
    if (projectViewPath?.isRegularFile() != true) {
      val projectViewPath = ProjectViewFileUtils.calculateProjectViewFilePath(project.rootDir, projectViewPath)
      project.setProjectViewPath(projectViewPath)
      openProjectViewInEditor(project, projectViewPath)
    }
  }
}
