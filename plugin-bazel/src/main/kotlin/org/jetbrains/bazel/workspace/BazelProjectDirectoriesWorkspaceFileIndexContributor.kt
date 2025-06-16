package org.jetbrains.bazel.workspace

import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.bazel.sdkcompat.registerOtherRootsCompat
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity

class BazelProjectDirectoriesWorkspaceFileIndexContributor : WorkspaceFileIndexContributor<BazelProjectDirectoriesEntity> {
  override val entityClass: Class<BazelProjectDirectoriesEntity> = BazelProjectDirectoriesEntity::class.java

  override fun registerFileSets(
    entity: BazelProjectDirectoriesEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    registrar.registerIncludedDirectories(entity)
    registrar.registerExcludedDirectories(entity)
    registrar.registerOtherRootsCompat(entity.projectRoot, entity.includedRoots, entity)
  }

  private fun WorkspaceFileSetRegistrar.registerIncludedDirectories(entity: BazelProjectDirectoriesEntity) =
    entity.includedRoots.forEach {
      registerFileSet(
        root = it,
        kind = WorkspaceFileKind.CONTENT,
        entity = entity,
        customData = null,
      )
    }

  private fun WorkspaceFileSetRegistrar.registerExcludedDirectories(entity: BazelProjectDirectoriesEntity) =
    entity.excludedRoots.forEach {
      registerExcludedRoot(
        excludedRoot = it,
        entity = entity,
      )
    }
}
