package org.jetbrains.bazel.golang.sync

import com.goide.project.GoModuleSettings
import com.intellij.openapi.application.edtWriteAction
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil
import org.jetbrains.bazel.sync.ProjectPostSyncHook

internal class GoModuleSupportPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    val workspaceModule = GoWorkspaceModuleUtil.findModule(project) ?: return
    edtWriteAction {
      GoModuleSettings.getInstance(workspaceModule).isGoSupportEnabled = true
    }
  }
}
