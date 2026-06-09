package org.jetbrains.bazel.flow.open

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectPreSyncHook

private val log = logger<RegenerateProjectViewFileContentPreSyncHook>()

internal class RegenerateProjectViewFileContentPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project

    val projectViewService = project.serviceAsync<ProjectViewService>()
    if (!projectViewService.allowExternalProjectViewModification) {
      log.info("RegenerateProjectViewFileContentPreSyncHook: skipping, external modification not allowed")
      return
    }

    var projectViewPath = project.bazelProjectSettings.projectViewPath
    log.info("RegenerateProjectViewFileContentPreSyncHook: projectViewPath=$projectViewPath, isFile=${projectViewPath?.isFile}")
    if (projectViewPath?.isFile == true) {
      val bazelbspProjectView = project.rootDir.findFileByRelativePath("${Constants.DOT_BAZELBSP_DIR_NAME}/${Constants.DEFAULT_PROJECT_VIEW_FILE_NAME}")
      log.info("RegenerateProjectViewFileContentPreSyncHook: bazelbspProjectView=$bazelbspProjectView")
      if (bazelbspProjectView == null || bazelbspProjectView == projectViewPath) {
        log.info("RegenerateProjectViewFileContentPreSyncHook: returning early, path already set to existing file")
        projectViewService.forceReparseCurrentProjectViewFiles()
        return
      }
      log.info("RegenerateProjectViewFileContentPreSyncHook: overriding $projectViewPath with $bazelbspProjectView")
      project.bazelProjectSettings = project.bazelProjectSettings.withNewProjectViewPath(bazelbspProjectView)
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
