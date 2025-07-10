package org.jetbrains.bazel.golang.sync

import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.legacy.WorkspaceModuleProjectSyncHook
import org.jetbrains.bsp.protocol.GoBuildTarget

class GoWorkspaceModuleEnable : WorkspaceModuleProjectSyncHook.EnableWorkspaceModuleSyncHookExtension {
  override suspend fun isEnabled(environment: ProjectSyncHook.ProjectSyncHookEnvironment): Boolean {
    val project = environment.project
    if (!project.isBazelProject) return false
    return BazelFeatureFlags.isGoSupportEnabled &&
      environment.server
        .workspaceBuildTargets()
        .targets
        .any { it.data is GoBuildTarget }
  }
}
