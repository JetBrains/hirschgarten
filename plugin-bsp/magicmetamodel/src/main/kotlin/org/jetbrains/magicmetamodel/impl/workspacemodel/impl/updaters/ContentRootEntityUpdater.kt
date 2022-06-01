package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.WorkspaceEntityStorageBuilder
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addContentRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class ContentRoot(
  val url: Path,
  val excludedUrls: List<Path> = emptyList(),
) : WorkspaceModelEntity()

internal class ContentRootEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<ContentRoot, ContentRootEntity> {

  override fun addEntity(entityToAdd: ContentRoot, parentModuleEntity: ModuleEntity): ContentRootEntity {
    return workspaceModelEntityUpdaterConfig.workspaceModel.updateProjectModel {
      addContentRootEntity(it, parentModuleEntity, entityToAdd)
    }
  }

  private fun addContentRootEntity(
    builder: WorkspaceEntityStorageBuilder,
    moduleEntity: ModuleEntity,
    entityToAdd: ContentRoot,
  ): ContentRootEntity = builder.addContentRootEntity(
    url = entityToAdd.url.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
    excludedUrls = entityToAdd.excludedUrls
      .map { it.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager) },
    excludedPatterns = DEFAULT_PATTERNS_URLS,
    module = moduleEntity,
  )

  private companion object {
    private val DEFAULT_PATTERNS_URLS = emptyList<String>()
  }
}
