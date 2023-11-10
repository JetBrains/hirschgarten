package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericSourceRoot
import org.jetbrains.workspacemodel.storage.BspEntitySource

internal open class SourceEntityUpdater(
  val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<GenericSourceRoot, SourceRootEntity> {
  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: GenericSourceRoot, parentModuleEntity: ModuleEntity): SourceRootEntity {
    val contentRootEntity = addContentRootEntity(entityToAdd, parentModuleEntity)

    return addSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      contentRootEntity,
      entityToAdd,
    )
  }

  private fun addContentRootEntity(
    entityToAdd: GenericSourceRoot,
    parentModuleEntity: ModuleEntity,
  ): ContentRootEntity {
    val contentRoot = ContentRoot(
      path = entityToAdd.sourcePath,
      excludedPaths = entityToAdd.excludedPaths,
    )

    return contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: GenericSourceRoot,
  ): SourceRootEntity =
    builder.addEntity(
      SourceRootEntity(
        url = entityToAdd.sourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        rootType = entityToAdd.rootType,
        entitySource = BspEntitySource,
      ) {
        this.contentRoot = contentRootEntity
      },
    )
}
