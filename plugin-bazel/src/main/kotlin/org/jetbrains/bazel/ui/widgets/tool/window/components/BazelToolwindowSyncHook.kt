package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.components.service
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.target.targetUtils

class BazelToolwindowSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    environment.project.service<BazelTargetsPanelModel>().updateTargets(environment.project.targetUtils.labelToTargetInfo)
  }
}
