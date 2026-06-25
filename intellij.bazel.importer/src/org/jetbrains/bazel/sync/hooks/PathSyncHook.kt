package org.jetbrains.bazel.sync.hooks

import com.intellij.openapi.components.serviceAsync
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.environment.BazelProjectContextService
import org.jetbrains.bazel.sync.withSubtask

internal class PathSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) =
    coroutineScope {
      environment.withSubtask("Collect bazel workspace info") {
        val projectCtxService = environment.project.serviceAsync<BazelProjectContextService>()
        val bazelInfo = environment.server.bazelInfo
        projectCtxService.bazelBinPath = bazelInfo.bazelBin
        projectCtxService.bazelExecPath = bazelInfo.execRoot
        projectCtxService.workspaceName = environment.workspace.workspaceName
        projectCtxService.bazelRelease = bazelInfo.release
      }
    }
}
