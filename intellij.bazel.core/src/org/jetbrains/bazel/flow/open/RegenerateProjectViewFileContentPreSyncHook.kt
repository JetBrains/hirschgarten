package org.jetbrains.bazel.flow.open

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectPreSyncHook

internal class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project

    val projectViewService = project.serviceAsync<ProjectViewService>()
    if (!projectViewService.allowExternalProjectViewModification) {
      return
    }

    var projectViewPath = project.bazelProjectSettings.projectViewPath
    if (projectViewPath?.isFile == true) {
      projectViewService.forceReparseCurrentProjectViewFiles()
      return
    }

    projectViewService.forceReparseCurrentProjectViewFiles()

    projectViewPath =
      ProjectViewFileUtils.calculateProjectViewFilePath(
        projectRootDir = project.rootDir,
        projectViewPath = projectViewPath?.toNioPathOrNull(),
      ).refreshAndFindVirtualFile() ?: return
    project.bazelProjectSettings = project.bazelProjectSettings
      .withNewProjectViewPath(projectViewPath)

    openProjectViewInEditor(project, projectViewPath)
  }
}
