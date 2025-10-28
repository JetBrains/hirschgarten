package org.jetbrains.bazel.flow.open

import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.settings.bazel.openProjectViewInEditor
import org.jetbrains.bazel.settings.bazel.setProjectViewPath
import org.jetbrains.bazel.sync.ProjectPreSyncHook

class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project

    val projectViewPath = project.bazelProjectSettings.projectViewPath
    if (projectViewPath?.isFile != true) {
      val projectViewPath = ProjectViewFileUtils.calculateProjectViewFilePath(project.rootDir, projectViewPath?.toNioPathOrNull())
      project.setProjectViewPath(projectViewPath)
      openProjectViewInEditor(project, projectViewPath)
    }
  }
}
