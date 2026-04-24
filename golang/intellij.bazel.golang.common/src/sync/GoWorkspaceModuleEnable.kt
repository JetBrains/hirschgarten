package org.jetbrains.bazel.golang.sync

import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.legacy.WorkspaceModuleProjectSyncHook
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget

internal class GoWorkspaceModuleEnable : WorkspaceModuleProjectSyncHook.EnableWorkspaceModuleSyncHookExtension {
  override suspend fun isEnabled(environment: ProjectSyncHook.ProjectSyncHookEnvironment): Boolean {
    val project = environment.project
    if (!project.isBazelProject) return false
    return BazelFeatureFlags.isGoSupportEnabled &&
      environment.workspace.targets
        .filter { it.id.isMainWorkspace }
        .mapNotNull { extractGoBuildTarget(it) }
        .any { it.generatedSources.isNotEmpty() }
  }
}
