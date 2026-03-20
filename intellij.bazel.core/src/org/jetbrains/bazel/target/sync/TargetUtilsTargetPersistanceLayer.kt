package org.jetbrains.bazel.target.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.environment.BazelTargetPersistenceLayer
import org.jetbrains.bazel.sync.environment.TargetPersistenceSpec
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.target.targetUtils

internal class TargetUtilsTargetPersistanceLayer : BazelTargetPersistenceLayer {
  override suspend fun saveAll(
    project: Project,
    spec: TargetPersistenceSpec,
  ) {
    project.targetUtils.saveTargets(
      targets = spec.targets,
      fileToTarget = spec.file2Target,
      libraryItems = spec.libraryItems,
    )
  }

  override suspend fun notifyAll(project: Project) {
    project.targetUtils.notifyTargetListUpdated()
  }
}
