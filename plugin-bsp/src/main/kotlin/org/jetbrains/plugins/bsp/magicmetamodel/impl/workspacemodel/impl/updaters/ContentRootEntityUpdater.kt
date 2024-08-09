package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ContentRoot

internal class ContentRootEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<ContentRoot, ContentRootEntity> {
  override fun addEntities(entitiesToAdd: List<ContentRoot>, parentModuleEntity: ModuleEntity): List<ContentRootEntity> =
    addContentRootEntities(
      parentModuleEntity,
      entitiesToAdd,
    )

  private fun addContentRootEntities(
    moduleEntity: ModuleEntity,
    entitiesToAdd: List<ContentRoot>,
  ): List<ContentRootEntity> {
    val contentRootEntities =
      entitiesToAdd.map { entityToAdd ->
        createContentRootEntity(moduleEntity, entityToAdd)
      }

    val updatedModuleEntity = workspaceModelEntityUpdaterConfig.withWorkspaceEntityStorageBuilder { builder ->
      builder.modifyModuleEntity(moduleEntity) {
        contentRoots += contentRootEntities
      }
    }

    return updatedModuleEntity.contentRoots.takeLast(contentRootEntities.size)
  }

  private fun createContentRootEntity(moduleEntity: ModuleEntity, entityToAdd: ContentRoot): ContentRootEntity.Builder {
    val url = entityToAdd.path.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
    val excludedUrls =
      entityToAdd.excludedPaths.map { it.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager) }
    val excludes =
      excludedUrls.map {
        ExcludeUrlEntity(
          url = it,
          entitySource = moduleEntity.entitySource,
        )
      }
    return ContentRootEntity(
      url = url,
      excludedPatterns = ArrayList(),
      entitySource = moduleEntity.entitySource,
    ) {
      this.excludedUrls = excludes
    }
  }

  override fun addEntity(entityToAdd: ContentRoot, parentModuleEntity: ModuleEntity): ContentRootEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
