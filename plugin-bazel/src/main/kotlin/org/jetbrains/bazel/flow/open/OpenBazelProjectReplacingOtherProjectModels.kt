package org.jetbrains.bazel.flow.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.BazelFeatureFlags

class OpenBazelProjectReplacingOtherProjectModels : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!BazelFeatureFlags.autoOpenProjectIfPresent) return

    val externalProjectPath = project.basePath.orEmpty()
    if (BazelUnlinkedProjectAware().isLinkedProject(project, externalProjectPath)) {
      // Already a Bazel project - nothing to do
      return
    }
    val projectFolder = findProjectFolderFromVFile(project.baseDir) ?: return
    BazelOpenProjectProvider().linkToExistingProjectAsync(projectFolder, project)
  }
}
