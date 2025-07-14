package org.jetbrains.bazel.golang.sync

import com.goide.project.GoModuleSettings
import com.intellij.openapi.application.writeAction
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.projectStructure.legacy.WorkspaceModuleUtils

class GoModuleSupportPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    val workspaceModule = WorkspaceModuleUtils.findModule(project) ?: return
    writeAction {
      GoModuleSettings.getInstance(workspaceModule).isGoSupportEnabled = true
    }
  }
}
