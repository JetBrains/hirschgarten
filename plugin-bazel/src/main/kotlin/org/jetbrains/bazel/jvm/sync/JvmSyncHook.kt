package org.jetbrains.bazel.jvm.sync

import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.target.sync.projectStructure.targetUtilsDiff

class JvmSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val targetUtilsDiff = environment.diff.targetUtilsDiff
    environment.withSubtask("Process JVM targets") {
      val task =
        CollectProjectDetailsTask(
          environment.project,
          it,
          environment.diff.workspaceModelDiff.mutableEntityStorage,
          targetUtilsDiff,
        )
      task.execute(
        project = environment.project,
        server = environment.server,
        progressReporter = environment.progressReporter,
        syncScope = environment.syncScope,
      )
      environment.diff.workspaceModelDiff.addPostApplyAction { task.postprocessingSubtask(targetUtilsDiff) }
    }
  }
}
