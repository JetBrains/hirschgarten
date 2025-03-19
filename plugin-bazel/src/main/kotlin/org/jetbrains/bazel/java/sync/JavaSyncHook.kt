package org.jetbrains.bazel.java.sync

import org.jetbrains.bazel.jvm.sync.CollectProjectDetailsTask
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.scope.FullProjectSync

class JavaSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val task = CollectProjectDetailsTask(environment.project, environment.taskId, environment.diff.workspaceModelDiff.mutableEntityStorage)
    task.execute(
      project = environment.project,
      server = environment.server,
      progressReporter = environment.progressReporter,
      baseTargetInfos = environment.baseTargetInfos,
      syncScope = environment.syncScope,
    )
    val targetListChanged = environment.syncScope is FullProjectSync
    environment.diff.workspaceModelDiff.addPostApplyAction { task.postprocessingSubtask(targetListChanged) }
  }
}
