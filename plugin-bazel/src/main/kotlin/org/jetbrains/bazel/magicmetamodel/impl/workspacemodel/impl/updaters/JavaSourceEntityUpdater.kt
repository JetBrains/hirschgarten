package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifySourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericSourceRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaSourceRoot

class JavaSourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  workspaceModelEntitiesFolderMarker: Boolean = false,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, JavaSourceRootPropertiesEntity> {
  private val sourceEntityUpdater = SourceEntityUpdater(workspaceModelEntityUpdaterConfig, workspaceModelEntitiesFolderMarker)
  private val bazelJavaSourceRootEntityUpdater = BazelJavaSourceRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override suspend fun addEntities(
    entitiesToAdd: List<JavaSourceRoot>,
    parentModuleEntity: ModuleEntity,
  ): List<JavaSourceRootPropertiesEntity> {
    bazelJavaSourceRootEntityUpdater.addEntities(entitiesToAdd)
    val sourceRootEntities =
      sourceEntityUpdater.addEntities(
        entitiesToAdd.map { entityToAdd ->
          GenericSourceRoot(
            entityToAdd.sourcePath,
            entityToAdd.rootType,
          )
        },
        parentModuleEntity,
      )

    return (sourceRootEntities zip entitiesToAdd).map { (sourceRootEntity, entityToAdd) ->
      addJavaSourceRootEntity(
        workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
        sourceRootEntity,
        entityToAdd,
      )
    }
  }

  private fun addJavaSourceRootEntity(
    builder: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
    entityToAdd: JavaSourceRoot,
  ): JavaSourceRootPropertiesEntity {
    val entity =
      JavaSourceRootPropertiesEntity(
        generated = entityToAdd.generated,
        packagePrefix = entityToAdd.packagePrefix,
        entitySource = sourceRoot.entitySource,
      )

    val updatedSourceRoot =
      builder.modifySourceRootEntity(sourceRoot) {
        this.javaSourceRoots = listOf(entity)
      }

    return updatedSourceRoot.javaSourceRoots.last()
  }

  override suspend fun addEntity(entityToAdd: JavaSourceRoot, parentModuleEntity: ModuleEntity): JavaSourceRootPropertiesEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
