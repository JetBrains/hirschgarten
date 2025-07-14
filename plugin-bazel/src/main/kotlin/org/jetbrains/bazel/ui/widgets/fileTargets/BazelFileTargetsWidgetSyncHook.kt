package org.jetbrains.bazel.ui.widgets.fileTargets

import org.jetbrains.bazel.sync.ProjectPostSyncHook

/**
 * A sync hook that updates the BazelFileTargetsWidget when a sync event occurs.
 */
private class BazelFileTargetsWidgetSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    updateBazelFileTargetsWidget(environment.project)
  }
}
