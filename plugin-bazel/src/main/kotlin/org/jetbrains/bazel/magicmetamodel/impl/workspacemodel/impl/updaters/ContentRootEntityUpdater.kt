package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot

class ContentRootEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<ContentRoot, ContentRootEntity> {
  override suspend fun addEntities(entitiesToAdd: List<ContentRoot>, parentModuleEntity: ModuleEntity): List<ContentRootEntity> =
    addContentRootEntities(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      parentModuleEntity,
      entitiesToAdd,
    )

  private suspend fun addContentRootEntities(
    builder: MutableEntityStorage,
    moduleEntity: ModuleEntity,
    entitiesToAdd: List<ContentRoot>,
  ): List<ContentRootEntity> {
    // Resolved together for better parallelization
    val resolvedContentRootsPaths =
      entitiesToAdd
        .map { it.path }
        .toResolvedVirtualFileUrls(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)

    val contentRootEntities =
      (entitiesToAdd zip resolvedContentRootsPaths).map { (entityToAdd, contentRootPath) ->
        createContentRootEntity(moduleEntity, entityToAdd, contentRootPath)
      }

    val updatedModuleEntity =
      builder.modifyModuleEntity(moduleEntity) {
        contentRoots += contentRootEntities
      }

    return updatedModuleEntity.contentRoots.takeLast(contentRootEntities.size)
  }

  private fun createContentRootEntity(
    moduleEntity: ModuleEntity,
    entityToAdd: ContentRoot,
    contentRootPath: VirtualFileUrl,
  ): ContentRootEntity.Builder {
    val excludedUrls =
      entityToAdd.excludedPaths.map { it.toResolvedVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager) }
    val excludes =
      excludedUrls.map {
        ExcludeUrlEntity(
          url = it,
          entitySource = moduleEntity.entitySource,
        )
      }
    return ContentRootEntity(
      url = contentRootPath,
      excludedPatterns = ArrayList(),
      entitySource = moduleEntity.entitySource,
    ) {
      this.excludedUrls = excludes
    }
  }

  override suspend fun addEntity(entityToAdd: ContentRoot, parentModuleEntity: ModuleEntity): ContentRootEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
