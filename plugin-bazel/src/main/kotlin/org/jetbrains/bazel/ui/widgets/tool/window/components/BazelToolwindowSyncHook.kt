package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.components.service
import org.jetbrains.bazel.sync.ProjectSyncHook

class BazelToolwindowSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val bspBuildTargets = environment.server.workspaceBuildTargets()
    environment.project.service<BazelTargetsPanelModel>().updateTargets(bspBuildTargets.targets.associateBy { it.id })
  }
}
