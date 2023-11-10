package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import org.jetbrains.workspacemodel.storage.BspEntitySource

internal class JavaResourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<ResourceRoot, JavaResourceRootPropertiesEntity> {
  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(
    entityToAdd: ResourceRoot,
    parentModuleEntity: ModuleEntity,
  ): JavaResourceRootPropertiesEntity {
    val contentRootEntity = addContentRootEntity(entityToAdd, parentModuleEntity)

    val sourceRoot = addSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      contentRootEntity,
      entityToAdd,
    )
    return addJavaResourceRootEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, sourceRoot)
  }

  private fun addContentRootEntity(
    entityToAdd: ResourceRoot,
    parentModuleEntity: ModuleEntity,
  ): ContentRootEntity {
    val contentRoot = ContentRoot(
      path = entityToAdd.resourcePath,
    )

    return contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: ResourceRoot,
  ): SourceRootEntity =
    builder.addEntity(
      SourceRootEntity(
        url = entityToAdd.resourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        rootType = ROOT_TYPE,
        entitySource = BspEntitySource,
      ) {
        this.contentRoot = contentRootEntity
      },
    )

  private fun addJavaResourceRootEntity(
    builder: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
  ): JavaResourceRootPropertiesEntity =
    builder.addEntity(
      JavaResourceRootPropertiesEntity(
        generated = DEFAULT_GENERATED,
        relativeOutputPath = DEFAULT_RELATIVE_OUTPUT_PATH,
        entitySource = sourceRoot.entitySource,
      ) {
        this.sourceRoot = sourceRoot
      },
    )

  private companion object {
    private const val DEFAULT_GENERATED = false
    private const val DEFAULT_RELATIVE_OUTPUT_PATH = ""

    private const val ROOT_TYPE = "java-resource"
  }
}
