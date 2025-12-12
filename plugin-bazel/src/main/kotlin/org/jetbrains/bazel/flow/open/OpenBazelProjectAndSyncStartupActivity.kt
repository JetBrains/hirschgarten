package org.jetbrains.bazel.flow.open

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.InitProjectActivity
import com.intellij.project.ProjectStoreOwner
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.settings.bazel.setProjectViewPath

// todo rename to ImportBazelProjectAndSyncStartupActivity
internal class OpenBazelProjectAndSyncStartupActivity : InitProjectActivity {
  override suspend fun run(project: Project) {
    // If flag is set and project is not yet linked as Bazel, link it first
    // This handles headless mode with -Dbazel.project.auto.open.if.present=true
    if (shouldLinkAsBazelProject(project)) {
      val projectFolder = findProjectFolderFromVFile(project.guessProjectDir()) ?: return
      BazelOpenProjectProvider().linkToExistingProjectAsync(projectFolder, project)
      // BazelStartupActivity will handle sync since isBazelProject will be true
      return
    }

    if (project !is ProjectStoreOwner) return

    val storeDescriptor = project.componentStore
      .storeDescriptor
    if (storeDescriptor !is BazelProjectStoreDescriptor) return

    // todo remove rootDir!
    // todo check workspace model emptiness
    if (project.serviceAsync<BazelProjectProperties>().rootDir != null) return

    // todo duplicates org.jetbrains.bazel.flow.open.BazelProjectOpenProcessor.calculateOpenProjectTask
    val path = storeDescriptor.projectIdentityFile
    val projectRootDir = findProjectFolderFromFile(path)!!

    val projectViewPath = getProjectViewPath(projectRootDir, path)
    if (projectViewPath != null) {
                          project.setProjectViewPath(projectViewPath)
    }

    project.initProperties(projectRootDir)
  }

  private fun shouldLinkAsBazelProject(project: Project): Boolean {
    val externalProjectPath = project.basePath.orEmpty()

    // Already linked - nothing to do
    if (BazelUnlinkedProjectAware().isLinkedProject(project, externalProjectPath)) return false

    // Link if flag is set
    if (BazelFeatureFlags.autoOpenProjectIfPresent) return true

    // Link if it's a Google Bazel project (.ijwb) and Google plugin is not installed
    val isGoogleBazelPluginEnabled = PluginManagerCore.getPluginSet().isPluginEnabled(PluginId.getId("com.google.idea.bazel.ijwb"))
    return !isGoogleBazelPluginEnabled && externalProjectPath.endsWith(".ijwb")
  }
}
