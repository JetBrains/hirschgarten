package org.jetbrains.plugins.bsp.flow.sync

import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.plugins.bsp.restOfTheFuckingHorse.server.tasks.CollectProjectDetailsTask

class CollectProjectDetailsSyncHook : ProjectSyncHook {
  override val buildToolId: BuildToolId = bspBuildToolId

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val task = CollectProjectDetailsTask(environment.project, environment.taskId, environment.diff.workspaceModelDiff.mutableEntityStorage)
    task.execute(
      server = environment.server,
      capabilities = environment.capabilities,
      progressReporter = environment.progressReporter,
      baseTargetInfos = environment.baseTargetInfos,
    )
    environment.diff.workspaceModelDiff.addPostApplyAction { task.postprocessingSubtask(environment.progressReporter) }
  }
}
