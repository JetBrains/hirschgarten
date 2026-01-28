package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot

class ContentRootEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<ContentRoot, ContentRootEntity> {
  override suspend fun addEntities(entitiesToAdd: List<ContentRoot>, parentModuleEntity: ModuleEntity): List<ContentRootEntity> {
    if (entitiesToAdd.isEmpty()) return emptyList()

    val (builder, virtualFileManager) = workspaceModelEntityUpdaterConfig
    val entitySource = parentModuleEntity.entitySource
    val contentRootEntities =
      entitiesToAdd
        .asSequence()
        .map {
          val url = it.path.toResolvedVirtualFileUrl(virtualFileManager)
          ContentRootEntity(
            url = url,
            excludedPatterns = emptyList(),
            entitySource = entitySource,
          ).apply {
            excludedUrls = it.excluded.map { file ->
              ExcludeUrlEntity(
                url = file.toResolvedVirtualFileUrl(virtualFileManager),
                entitySource = entitySource,
              )
            }
          }
        }.toList()

    val updatedModuleEntity =
      builder.modifyModuleEntity(parentModuleEntity) {
        contentRoots += contentRootEntities
      }

    return updatedModuleEntity.contentRoots.takeLast(contentRootEntities.size)
  }

  override suspend fun addEntity(entityToAdd: ContentRoot, parentModuleEntity: ModuleEntity): ContentRootEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
