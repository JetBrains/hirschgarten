package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addContentRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class ContentRoot(
  val url: Path,
  val excludedUrls: List<Path> = ArrayList(),
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
    val toExclude = if (moduleEntity.name == "bsp-workspace-root") listOf("bazel-*") else DEFAULT_PATTERNS_URLS // todo this should be done only for bazelbsp projects
    return builder.addContentRootEntity(
      url = entityToAdd.url.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
      excludedUrls = entityToAdd.excludedUrls
        .map { it.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager) },
      excludedPatterns = toExclude,
      module = moduleEntity,
    )
  }

  private companion object {
    private val DEFAULT_PATTERNS_URLS = ArrayList<String>()
  }
}
