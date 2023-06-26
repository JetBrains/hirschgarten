package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class ContentRoot(
  val url: Path,
  val excludedPaths: List<Path> = ArrayList(),
) : WorkspaceModelEntity()

internal class ContentRootEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<ContentRoot, ContentRootEntity> {

  override fun addEntity(entityToAdd: ContentRoot, parentModuleEntity: ModuleEntity): ContentRootEntity =
    addContentRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      parentModuleEntity,
      entityToAdd
    )

  private fun addContentRootEntity(
    builder: MutableEntityStorage,
    moduleEntity: ModuleEntity,
    entityToAdd: ContentRoot,
  ): ContentRootEntity {
    val url = entityToAdd.url.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
    val excludedUrls = entityToAdd.excludedPaths
            .map { it.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager) }
    val excludes = excludedUrls.map {
      builder.addEntity(
        ExcludeUrlEntity(
          url = it,
          entitySource = moduleEntity.entitySource
        )
      )
    }
    return builder.addEntity(
      ContentRootEntity(
        url = url,
        excludedPatterns = ArrayList(),
        entitySource = moduleEntity.entitySource
      ) {
        this.excludedUrls = excludes
        this.module = moduleEntity
      }
    )
  }
}
