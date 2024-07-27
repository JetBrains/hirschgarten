package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ResourceRoot

internal class PythonResourceEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<ResourceRoot, SourceRootEntity> {
  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntities(entitiesToAdd: List<ResourceRoot>, parentModuleEntity: ModuleEntity): List<SourceRootEntity> {
    val contentRootEntities = addContentRootEntities(entitiesToAdd, parentModuleEntity)

    return (entitiesToAdd zip contentRootEntities).map { (entityToAdd, contentRootEntity) ->
      addSourceRootEntity(
        workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
        contentRootEntity,
        entityToAdd,
      )
    }
  }

  private fun addContentRootEntities(entitiesToAdd: List<ResourceRoot>, parentModuleEntity: ModuleEntity): List<ContentRootEntity> {
    val contentRoots =
      entitiesToAdd.map { entityToAdd ->
        ContentRoot(
          path = entityToAdd.resourcePath,
        )
      }

    return contentRootEntityUpdater.addEntities(contentRoots, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: ResourceRoot,
  ): SourceRootEntity {
    val entity =
      SourceRootEntity(
        url = entityToAdd.resourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        rootTypeId = ROOT_TYPE,
        entitySource = contentRootEntity.entitySource,
      )

    val updatedContentRootEntity =
      builder.modifyContentRootEntity(contentRootEntity) {
        this.sourceRoots += entity
      }

    return updatedContentRootEntity.sourceRoots.last()
  }

  override fun addEntity(entityToAdd: ResourceRoot, parentModuleEntity: ModuleEntity): SourceRootEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()

  private companion object {
    private val ROOT_TYPE = SourceRootTypeId("python-resource")
  }
}
