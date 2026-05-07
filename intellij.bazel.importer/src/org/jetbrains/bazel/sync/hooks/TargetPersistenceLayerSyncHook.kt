package org.jetbrains.bazel.sync.hooks

import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.environment.TargetPersistenceSpec
import org.jetbrains.bazel.sync.environment.projectCtx

// TODO: rename
internal class TargetPersistenceLayerSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val project = environment.project
    val targetPersistanceLayer = project.projectCtx.targetPersistenceLayer
    val workspace = environment.workspace

    val spec = TargetPersistenceSpec(
      targets = workspace.targets,
      file2Target = workspace.fileToTarget.mapValues { it -> it.value.map { it.id }.distinct() },
    )
    targetPersistanceLayer.saveAll(project, spec)
    targetPersistanceLayer.notifyAll(project)
  }
}
