package org.jetbrains.plugins.bsp.projectDetails

import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.FullProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff

class CollectProjectDetailsSyncHook : ProjectSyncHook {
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
