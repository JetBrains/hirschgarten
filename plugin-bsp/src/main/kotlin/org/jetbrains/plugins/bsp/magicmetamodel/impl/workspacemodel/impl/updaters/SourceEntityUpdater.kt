package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericSourceRoot

internal open class SourceEntityUpdater(val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<GenericSourceRoot, SourceRootEntity> {
  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntities(entitiesToAdd: List<GenericSourceRoot>, parentModuleEntity: ModuleEntity): List<SourceRootEntity> {
    val contentRootEntities = addContentRootEntities(entitiesToAdd, parentModuleEntity)

    return (contentRootEntities zip entitiesToAdd).map { (contentRootEntity, entryToAdd) ->
      addSourceRootEntity(
        workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
        contentRootEntity,
        entryToAdd,
      )
    }
  }

  private fun addContentRootEntities(entitiesToAdd: List<GenericSourceRoot>, parentModuleEntity: ModuleEntity): List<ContentRootEntity> {
    val contentRoots =
      entitiesToAdd.map { entityToAdd ->
        ContentRoot(
          path = entityToAdd.sourcePath,
          excludedPaths = entityToAdd.excludedPaths,
        )
      }

    return contentRootEntityUpdater.addEntities(contentRoots, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: GenericSourceRoot,
  ): SourceRootEntity {
    val entity =
      SourceRootEntity(
        url = entityToAdd.sourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        rootTypeId = entityToAdd.rootType,
        entitySource = contentRootEntity.entitySource,
      )

    val updatedContentRootEntity =
      builder.modifyContentRootEntity(contentRootEntity) {
        this.sourceRoots += entity
      }

    return updatedContentRootEntity.sourceRoots.last()
  }

  override fun addEntity(entityToAdd: GenericSourceRoot, parentModuleEntity: ModuleEntity): SourceRootEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
