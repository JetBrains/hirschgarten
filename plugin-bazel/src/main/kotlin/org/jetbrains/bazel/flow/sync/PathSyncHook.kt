package org.jetbrains.bazel.flow.sync

import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.workspaceName
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask

class PathSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) =
    coroutineScope {
      environment.withSubtask("Collect bazel workspace info") {
        val bazelBinPathService = BazelBinPathService.getInstance(environment.project)
        val bazelBinPathResult =
          query("workspace/bazelPaths") { environment.server.workspaceBazelPaths() }
        bazelBinPathService.bazelBinPath = bazelBinPathResult.bazelBin
        bazelBinPathService.bazelExecPath = bazelBinPathResult.executionRoot
        val bazelWorkspaceResult =
          query("workspace/bazelWorkspaceName") { environment.server.workspaceName() }

        environment.project.workspaceName = bazelWorkspaceResult.workspaceName
      }
    }
}
