package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifySourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericSourceRoot
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaSourceRoot

internal class JavaSourceEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, JavaSourceRootPropertiesEntity> {
  private val sourceEntityUpdater = SourceEntityUpdater(workspaceModelEntityUpdaterConfig)
  private val generatedJavaSourceEntityUpdater = GeneratedJavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntities(entitiesToAdd: List<JavaSourceRoot>, parentModuleEntity: ModuleEntity): List<JavaSourceRootPropertiesEntity> {
    generatedJavaSourceEntityUpdater.addEntities(entitiesToAdd)
    val sourceRootEntities =
      sourceEntityUpdater.addEntities(
        entitiesToAdd.map { entityToAdd ->
          GenericSourceRoot(
            entityToAdd.sourcePath,
            entityToAdd.rootType,
            entityToAdd.excludedPaths,
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

  override fun addEntity(entityToAdd: JavaSourceRoot, parentModuleEntity: ModuleEntity): JavaSourceRootPropertiesEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
