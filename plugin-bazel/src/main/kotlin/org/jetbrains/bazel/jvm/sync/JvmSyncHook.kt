package org.jetbrains.bazel.jvm.sync

import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.withSubtask

internal class JvmSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Process JVM targets") {
      val task =
        CollectProjectDetailsTask(
          environment.project,
          it,
          environment.diff,
        )
      task.execute(
        project = environment.project,
        workspace = environment.workspace,
        server = environment.server,
        progressReporter = environment.progressReporter,
        syncScope = environment.syncScope,
      )
      environment.deferredApplyActions += { task.postprocessingSubtask(environment.workspace) }
    }
  }
}
