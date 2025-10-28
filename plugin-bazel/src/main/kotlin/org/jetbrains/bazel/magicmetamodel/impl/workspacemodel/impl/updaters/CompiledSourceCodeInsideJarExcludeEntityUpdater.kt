package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.storage.entities
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId
import org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity

class CompiledSourceCodeInsideJarExcludeEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithoutParentModuleUpdater<CompiledSourceCodeInsideJarExclude, CompiledSourceCodeInsideJarExcludeEntity> {
  override suspend fun addEntity(entityToAdd: CompiledSourceCodeInsideJarExclude): CompiledSourceCodeInsideJarExcludeEntity {
    val currentExcludeEntity =
      WorkspaceModel
        .getInstance(workspaceModelEntityUpdaterConfig.project)
        .currentSnapshot
        .entities<CompiledSourceCodeInsideJarExcludeEntity>()
        .firstOrNull()

    val excludeEntityId =
      if (currentExcludeEntity == null) {
        0
      } else if (currentExcludeEntity.relativePathsInsideJarToExclude == entityToAdd.relativePathsInsideJarToExclude &&
        currentExcludeEntity.librariesFromInternalTargetsUrls == entityToAdd.librariesFromInternalTargetsUrls
      ) {
        currentExcludeEntity.excludeId.id
      } else {
        // Change the ID, so that all the referring entities' data (LibraryCompiledSourceCodeInsideJarExcludeEntity) will be changed,
        // and therefore CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor will be rerun on them.
        currentExcludeEntity.excludeId.id + 1
      }

    val excludeEntity =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
        CompiledSourceCodeInsideJarExcludeEntity(
          relativePathsInsideJarToExclude = entityToAdd.relativePathsInsideJarToExclude,
          librariesFromInternalTargetsUrls = entityToAdd.librariesFromInternalTargetsUrls,
          excludeId = CompiledSourceCodeInsideJarExcludeId(excludeEntityId),
          entitySource = BazelProjectEntitySource,
        ),
      )

    val libraries = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.entities<LibraryEntity>().toList()
    val libraryExcludeEntities =
      libraries.map { library ->
        LibraryCompiledSourceCodeInsideJarExcludeEntity(
          library.symbolicId,
          excludeEntity.symbolicId,
          entitySource = BazelProjectEntitySource,
        )
      }
    for (libraryExcludeEntity in libraryExcludeEntities) {
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(libraryExcludeEntity)
    }
    return excludeEntity
  }
}
