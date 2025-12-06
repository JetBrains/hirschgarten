package org.jetbrains.bazel.flow.open

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.startup.initializeBazelSyncEnvironment
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask

class OpenBazelProjectReplacingOtherProjectModels : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!BazelFeatureFlags.autoOpenProjectIfPresent && !shouldImportGoogleBazelProject(project)) return

    val externalProjectPath = project.basePath.orEmpty()
    if (BazelUnlinkedProjectAware().isLinkedProject(project, externalProjectPath)) {
      // Already a Bazel project - nothing to do
      return
    }
    val projectFolder = findProjectFolderFromVFile(project.baseDir) ?: return
    BazelOpenProjectProvider().linkToExistingProjectAsync(projectFolder, project)

    initializeBazelSyncEnvironment()
    ProjectSyncTask(project).sync(
      syncScope = SecondPhaseSync,
      buildProject = BazelFeatureFlags.isBuildProjectOnSyncEnabled,
    )
  }

  private fun shouldImportGoogleBazelProject(project: Project) =
    !isGoogleBazelPluginEnabled() && project.basePath.orEmpty().endsWith(".ijwb")

  private fun isGoogleBazelPluginEnabled(): Boolean =
    PluginManagerCore.getPluginSet().isPluginEnabled(PluginId.getId("com.google.idea.bazel.ijwb"))
}
