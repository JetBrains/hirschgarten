package org.jetbrains.bazel.golang.sync

import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.legacy.WorkspaceModuleProjectSyncHook
import org.jetbrains.bazel.target.sync.projectStructure.targetUtilsDiff
import org.jetbrains.bsp.protocol.GoBuildTarget

class GoWorkspaceModuleEnable : WorkspaceModuleProjectSyncHook.EnableWorkspaceModuleSyncHookExtension {
  override suspend fun isEnabled(environment: ProjectSyncHook.ProjectSyncHookEnvironment): Boolean {
    val project = environment.project
    if (!project.isBazelProject) return false
    return BazelFeatureFlags.isGoSupportEnabled &&
      environment.diff.targetUtilsDiff.bspTargets
        .filter { it.id.isMainWorkspace }
        .mapNotNull { it.data as? GoBuildTarget }
        .any { it.generatedSources.isNotEmpty() }
  }
}
