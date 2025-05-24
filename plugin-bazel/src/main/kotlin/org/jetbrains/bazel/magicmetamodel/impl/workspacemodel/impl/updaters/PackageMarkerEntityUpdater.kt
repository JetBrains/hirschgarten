package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity
import org.jetbrains.bazel.workspacemodel.entities.packageMarkerEntities

class PackageMarkerEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, PackageMarkerEntity> {
  override suspend fun addEntities(entitiesToAdd: List<JavaSourceRoot>, parentModuleEntity: ModuleEntity): List<PackageMarkerEntity> {
    val entities =
      entitiesToAdd.map { entityToAdd ->
        PackageMarkerEntity(
          root = entityToAdd.sourcePath.toResolvedVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
          packagePrefix = entityToAdd.packagePrefix,
          entitySource = BazelDummyEntitySource,
        )
      }
    workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.modifyModuleEntity(parentModuleEntity) {
      this.packageMarkerEntities += entities
    }
    return parentModuleEntity.packageMarkerEntities
  }

  override suspend fun addEntity(entityToAdd: JavaSourceRoot, parentModuleEntity: ModuleEntity): PackageMarkerEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
