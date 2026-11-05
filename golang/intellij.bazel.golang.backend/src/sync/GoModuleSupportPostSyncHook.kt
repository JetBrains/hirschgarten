package org.jetbrains.bazel.golang.sync

import com.goide.project.GoModuleSettings
import com.goide.vgo.configuration.VgoProjectSettings
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil
import org.jetbrains.bazel.sync.ProjectPostSyncHook

internal class GoModuleSupportPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project

    // Bazel downloads dependencies on its own, so tracking go.mod files for changed dependencies doesn't make sense
    VgoProjectSettings.getInstance(project).isIntegrationEnabled = false

    val workspaceModule = GoWorkspaceModuleUtil.findModule(project) ?: return
    // This should come after disabling go.mod integration, otherwise this will spam the UI with "go list"/"go mod download"
    GoModuleSettings.getInstance(workspaceModule).isGoSupportEnabled = true
  }
}
