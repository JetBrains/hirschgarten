package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericSourceRoot

internal open class SourceEntityUpdater(
  val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  val workspaceModelEntityFolderMarkerExists: Boolean = false,
) : WorkspaceModelEntityWithParentModuleUpdater<GenericSourceRoot, SourceRootEntity> {
  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override suspend fun addEntities(entitiesToAdd: List<GenericSourceRoot>, parentModuleEntity: ModuleEntity): List<SourceRootEntity> {
    return if (workspaceModelEntityFolderMarkerExists) {
      val commonContentRoot = addSingleContentRootEntity(entitiesToAdd, parentModuleEntity) ?: return emptyList()
      entitiesToAdd.map { addSourceRootEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, commonContentRoot, it) }
    } else {
      val contentRootEntities = addContentRootEntities(entitiesToAdd, parentModuleEntity)
      (contentRootEntities zip entitiesToAdd).map { (contentRootEntity, entryToAdd) ->
        addSourceRootEntity(
          workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
          contentRootEntity,
          entryToAdd,
        )
      }
    }
  }

  /**
   * this is specifically used for the workspacemodel module in hirschgarten
   */
  private suspend fun addSingleContentRootEntity(
    entitiesToAdd: List<GenericSourceRoot>,
    parentModuleEntity: ModuleEntity,
  ): ContentRootEntity? {
    if (entitiesToAdd.isEmpty()) return null

    val commonContentRoot = calculateCommonContentRoot(entitiesToAdd) ?: return null

    return contentRootEntityUpdater.addEntity(commonContentRoot, parentModuleEntity)
  }

  private fun calculateCommonContentRoot(sourceRoots: List<GenericSourceRoot>) =
    when (sourceRoots.size) {
      0 -> null
      else ->
        ContentRoot(path = sourceRoots.first().sourcePath.parent)
    }

  private suspend fun addContentRootEntities(
    entitiesToAdd: List<GenericSourceRoot>,
    parentModuleEntity: ModuleEntity,
  ): List<ContentRootEntity> {
    val contentRoots =
      entitiesToAdd.map { entityToAdd ->
        ContentRoot(path = entityToAdd.sourcePath)
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

  override suspend fun addEntity(entityToAdd: GenericSourceRoot, parentModuleEntity: ModuleEntity): SourceRootEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()
}
