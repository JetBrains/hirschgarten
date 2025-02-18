package org.jetbrains.bazel.workspace

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.bazel.workspacemodel.entities.BspProjectDirectoriesEntity

class BspProjectDirectoriesWorkspaceFileIndexContributor : WorkspaceFileIndexContributor<BspProjectDirectoriesEntity> {
  override val entityClass: Class<BspProjectDirectoriesEntity> = BspProjectDirectoriesEntity::class.java

  override fun registerFileSets(
    entity: BspProjectDirectoriesEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    registrar.registerIncludedDirectories(entity)
    registrar.registerExcludedDirectories(entity)
    registrar.registerAllOtherDirectoriesAsExcluded(entity)
  }

  private fun WorkspaceFileSetRegistrar.registerIncludedDirectories(entity: BspProjectDirectoriesEntity) =
    entity.includedRoots.forEach {
      registerFileSet(
        root = it,
        kind = WorkspaceFileKind.CONTENT,
        entity = entity,
        customData = null,
      )
    }

  private fun WorkspaceFileSetRegistrar.registerExcludedDirectories(entity: BspProjectDirectoriesEntity) =
    entity.excludedRoots.forEach {
      registerExcludedRoot(
        excludedRoot = it,
        entity = entity,
      )
    }

  private fun WorkspaceFileSetRegistrar.registerAllOtherDirectoriesAsExcluded(entity: BspProjectDirectoriesEntity) {
    val includedRoots = entity.includedRoots.mapNotNull { it.virtualFile }.toSet()
    registerExclusionCondition(
      root = entity.projectRoot,
      condition = { !VfsUtilCore.isUnderFiles(it, includedRoots) },
      entity = entity,
    )
  }
}
