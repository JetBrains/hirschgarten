package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.JavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class SourceRoot(
  val sourceDir: Path,
  val generated: Boolean
) : WorkspaceModelEntity()

internal data class JavaSourceRoot(
  val sourceDir: Path,
  val generated: Boolean,
  val packagePrefix: String,
  val rootType: String,
  val excludedFiles: List<Path> = ArrayList(),
) : WorkspaceModelEntity()

internal class JavaSourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, JavaSourceRootEntity> {

  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: JavaSourceRoot, parentModuleEntity: ModuleEntity): JavaSourceRootEntity {
    val contentRootEntity = addContentRootEntity(entityToAdd, parentModuleEntity)

    val sourceRootEntity = addSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      contentRootEntity,
      entityToAdd
    )
    return addJavaSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      sourceRootEntity,
      entityToAdd
    )
  }

  private fun addContentRootEntity(
    entityToAdd: JavaSourceRoot,
    parentModuleEntity: ModuleEntity
  ): ContentRootEntity {
    val contentRoot = ContentRoot(
      url = entityToAdd.sourceDir,
      excludedUrls = entityToAdd.excludedFiles,
    )

    return contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: JavaSourceRoot,
  ): SourceRootEntity = builder.addSourceRootEntity(
    contentRoot = contentRootEntity,
    url = entityToAdd.sourceDir.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
    rootType = entityToAdd.rootType,
    source = DoNotSaveInDotIdeaDirEntitySource,
  )

  private fun addJavaSourceRootEntity(
    builder: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
    entityToAdd: JavaSourceRoot,
  ): JavaSourceRootEntity = builder.addJavaSourceRootEntity(
    sourceRoot = sourceRoot,
    generated = entityToAdd.generated,
    packagePrefix = entityToAdd.packagePrefix,
  )
}
