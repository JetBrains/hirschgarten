package org.jetbrains.bazel.flow.open

import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectPreSyncHook

class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project

    var projectViewPath = project.bazelProjectSettings.projectViewPath
    if (projectViewPath?.isFile == true) return

    projectViewPath = ProjectViewFileUtils.calculateProjectViewFilePath(
      projectRootDir = project.rootDir,
      projectViewPath = projectViewPath?.toNioPathOrNull(),
    ).refreshAndFindVirtualFile()!!
    project.bazelProjectSettings = project.bazelProjectSettings
      .withNewProjectViewPath(projectViewPath)

    openProjectViewInEditor(project, projectViewPath)
  }
}
