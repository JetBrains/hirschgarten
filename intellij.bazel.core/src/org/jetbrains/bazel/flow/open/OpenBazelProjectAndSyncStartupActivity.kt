package org.jetbrains.bazel.flow.open

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.InitProjectActivity
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.workspaceModel.ide.registerProjectRoot
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService
import org.jetbrains.bazel.flow.vcs.BazelVcsRootErrorFilter
import org.jetbrains.bazel.project.DefaultProjectViewService

internal class OpenBazelProjectAndSyncStartupActivity : InitProjectActivity {
  override suspend fun run(project: Project) {
    // If flag is set and project is not yet linked as Bazel, link it first
    // This handles headless mode with -Dbazel.project.auto.open.if.present=true
    if (shouldLinkAsBazelProject(project)) {
      logger.trace { "Auto-linking Bazel project: ${project.name}" }
      val projectFolder = findProjectFolderFromVFile(project.guessProjectDir()) ?: return
      val workspaceFile = projectFolder.toNioPathOrNull()?.workspaceFile ?: return
      BazelApplicationCoroutineScopeService.getInstance().launch {
        closeAndReopenAsBazelProject(project, workspaceFile)
      }
      return
    }

    if (project.isBazelProject) {
      doBazelProjectInitialization(project)
    }
  }

  private fun shouldLinkAsBazelProject(project: Project): Boolean {
    // Already linked - nothing to do
    if (project.isBazelProject) return false
    if (PluginManagerCore.getPluginSet().isPluginEnabled(PluginId.getId("com.google.idea.bazel.ijwb"))) return false
    // Link if flag is set
    return BazelFeatureFlags.autoOpenProjectIfPresent
  }

  private suspend fun doBazelProjectInitialization(project: Project) {
    // After project opening refactor Bazel plugin assumes that project view is available
    // immediately after project opening, because now the plugin literally "opens a project view".
    // Make sure that the file is loaded and available before project opens.
    DefaultProjectViewService.getInstance(project).ensureProjectViewInitialized()

    // Make sure that Bazel project has at least one root registered at startup, so
    // user can start working even when project isn't synced yet.
    val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()
    registerProjectRoot(project, project.rootDir.toVirtualFileUrl(virtualFileUrlManager))

    // An earlier session may have persisted a Git mapping in a Bazel output directory in vcs.xml (BAZEL-948), e.g.,
    // the execution root or the bazel-<workspace> symlink. Remove it here, before the VCS initialization activates Git for it,
    // as Git over the whole Bazel output tree can freeze the IDE. This is the only cleanup: BazelVcsRootErrorFilter keeps
    // new such mappings from being auto-registered. Mappings outside the Bazel output directories stay untouched.
    // It has to be this activity, since projectPreInit accepts only approved classes (see ProjectManagerImpl.runApprovedExtensions).
    BazelVcsRootErrorFilter.removeBazelOutputMappings(project)
  }

  companion object {
    private val logger = Logger.getInstance(OpenBazelProjectAndSyncStartupActivity::class.java)
  }
}
