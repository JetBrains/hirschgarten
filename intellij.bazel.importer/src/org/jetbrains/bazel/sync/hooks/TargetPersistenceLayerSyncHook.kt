package org.jetbrains.bazel.sync.hooks

import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.environment.TargetPersistenceSpec
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget

// TODO: rename
internal class TargetPersistenceLayerSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val project = environment.project
    val targetPersistanceLayer = project.projectCtx.targetPersistenceLayer
    val workspace = environment.workspace

    val libraryNameToModule = HashMap<String, Label>()
    for (target in workspace.targets) {
      val libraries = extractJvmBuildTarget(target)?.libraries ?: continue
      for (library in libraries) {
        if (library.id.isSynthetic)
          continue
        libraryNameToModule[library.id.formatAsModuleName(project)] = target.id
      }
    }

    val spec = TargetPersistenceSpec(
        targets = workspace.targets,
        libraryNameToModule = libraryNameToModule,
        file2Target = workspace.fileToTarget,
    )
    targetPersistanceLayer.saveAll(project, spec)
    targetPersistanceLayer.notifyAll(project)
  }
}
