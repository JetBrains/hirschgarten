package org.jetbrains.bazel.ui.widgets.fileTargets

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment

/**
 * A sync hook that updates the BazelFileTargetsWidget when a sync event occurs.
 */
class BazelFileTargetsWidgetSyncHook : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = true

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val project = environment.project
    ApplicationManager.getApplication().invokeLater {
      project.updateBazelFileTargetsWidget()
    }
  }
}
