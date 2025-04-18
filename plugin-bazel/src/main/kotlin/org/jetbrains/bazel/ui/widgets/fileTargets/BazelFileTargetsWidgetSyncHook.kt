package org.jetbrains.bazel.ui.widgets.fileTargets

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.bazel.sync.ProjectPostSyncHook

/**
 * A sync hook that updates the BazelFileTargetsWidget when a sync event occurs.
 */
class BazelFileTargetsWidgetSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    ApplicationManager.getApplication().invokeLater {
      project.updateBazelFileTargetsWidget()
    }
  }
}
