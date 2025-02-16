package org.jetbrains.bazel.jvm.sync

import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.config.bspBuildToolId
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.scope.FullProjectSync

class JvmSyncHook : ProjectSyncHook {
  override val buildToolId: BuildToolId = bspBuildToolId

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val task = CollectProjectDetailsTask(environment.project, environment.taskId, environment.diff.workspaceModelDiff.mutableEntityStorage)
    task.execute(
      project = environment.project,
      server = environment.server,
      capabilities = environment.capabilities,
      progressReporter = environment.progressReporter,
      baseTargetInfos = environment.baseTargetInfos,
      syncScope = environment.syncScope,
    )
    val targetListChanged = environment.syncScope is FullProjectSync
    environment.diff.workspaceModelDiff.addPostApplyAction { task.postprocessingSubtask(targetListChanged) }
  }
}
