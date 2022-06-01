package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.WorkspaceEntityStorageBuilder
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addSourceRootEntity
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
  val excludedFiles: List<Path> = emptyList(),
) : WorkspaceModelEntity()

internal class JavaSourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, JavaSourceRootEntity> {

  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: JavaSourceRoot, parentModuleEntity: ModuleEntity): JavaSourceRootEntity {
    val contentRootEntity = addContentRootEntity(entityToAdd, parentModuleEntity)

    return workspaceModelEntityUpdaterConfig.workspaceModel.updateProjectModel {
      val sourceRootEntity = addSourceRootEntity(it, contentRootEntity, entityToAdd)
      addJavaSourceRootEntity(it, sourceRootEntity, entityToAdd)
    }
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
    builder: WorkspaceEntityStorageBuilder,
    contentRootEntity: ContentRootEntity,
    entityToAdd: JavaSourceRoot,
  ): SourceRootEntity = builder.addSourceRootEntity(
    contentRoot = contentRootEntity,
    url = entityToAdd.sourceDir.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
    rootType = ROOT_TYPE,
    source = workspaceModelEntityUpdaterConfig.projectConfigSource,
  )

  private fun addJavaSourceRootEntity(
    builder: WorkspaceEntityStorageBuilder,
    sourceRoot: SourceRootEntity,
    entityToAdd: JavaSourceRoot,
  ): JavaSourceRootEntity = builder.addJavaSourceRootEntity(
    sourceRoot = sourceRoot,
    generated = entityToAdd.generated,
    packagePrefix = entityToAdd.packagePrefix,
  )

  private companion object {
    private const val ROOT_TYPE = "java-source"
  }
}
