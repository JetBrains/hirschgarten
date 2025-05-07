package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.components.service
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.withSubtask

class BazelToolwindowSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    environment.withSubtask("Update Targets Panel") {
      val bspBuildTargets = environment.server.workspaceBuildTargets()
      environment.project.service<BazelTargetsPanelModel>().updateTargets(bspBuildTargets.targets.associateBy { it.id })
    }
  }
}
