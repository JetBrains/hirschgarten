package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaResourceRootPropertiesEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class JavaResourceRoot(
  val resourcePath: Path,
) : WorkspaceModelEntity()

internal class JavaResourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaResourceRoot, JavaResourceRootPropertiesEntity> {

  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(
    entityToAdd: JavaResourceRoot,
    parentModuleEntity: ModuleEntity
  ): JavaResourceRootPropertiesEntity {
    val contentRootEntity = addContentRootEntity(entityToAdd, parentModuleEntity)

    val sourceRoot = addSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      contentRootEntity,
      entityToAdd
    )
    return addJavaResourceRootEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, sourceRoot)
  }

  private fun addContentRootEntity(
    entityToAdd: JavaResourceRoot,
    parentModuleEntity: ModuleEntity
  ): ContentRootEntity {
    val contentRoot = ContentRoot(
      url = entityToAdd.resourcePath
    )

    return contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: JavaResourceRoot,
  ): SourceRootEntity = builder.addSourceRootEntity(
    contentRoot = contentRootEntity,
    url = entityToAdd.resourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
    rootType = ROOT_TYPE,
    source = DoNotSaveInDotIdeaDirEntitySource,
  )

  private fun addJavaResourceRootEntity(
    builder: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
  ): JavaResourceRootPropertiesEntity = builder.addJavaResourceRootEntity(
    sourceRoot = sourceRoot,
    generated = DEFAULT_GENERATED,
    relativeOutputPath = DEFAULT_RELATIVE_OUTPUT_PATH,
  )

  private companion object {
    private const val DEFAULT_GENERATED = false
    private const val DEFAULT_RELATIVE_OUTPUT_PATH = ""

    private const val ROOT_TYPE = "java-resource"
  }
}
