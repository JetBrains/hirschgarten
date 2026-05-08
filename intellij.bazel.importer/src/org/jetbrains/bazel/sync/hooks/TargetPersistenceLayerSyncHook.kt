package org.jetbrains.bazel.sync.hooks

import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.environment.TargetPersistenceSpec
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.scope.PartialProjectSync

// TODO: rename
internal class TargetPersistenceLayerSyncHook : ProjectSyncHook {
  override fun supportsPartialSync(): Boolean = true

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val project = environment.project
    val targetPersistanceLayer = project.projectCtx.targetPersistenceLayer
    val workspace = environment.workspace

    val spec = TargetPersistenceSpec(
        targets = workspace.targets,
        file2Target = workspace.fileToTarget,
    )
    when (val syncScope = environment.syncScope) {
      is PartialProjectSync -> {
        val targetsToReplace = syncScope.targetsToSync + workspace.targets.map { it.id }
        targetPersistanceLayer.mergePartial(project, targetsToReplace, spec)
      }
      else -> targetPersistanceLayer.saveAll(project, spec)
    }
    targetPersistanceLayer.notifyAll(project)
  }
}
