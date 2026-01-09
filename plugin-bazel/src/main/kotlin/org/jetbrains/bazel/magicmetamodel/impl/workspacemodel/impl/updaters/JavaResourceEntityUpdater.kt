package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.jps.entities.modifySourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot

class JavaResourceEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<ResourceRoot, JavaResourceRootPropertiesEntity> {
  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override suspend fun addEntities(
    entitiesToAdd: List<ResourceRoot>,
    parentModuleEntity: ModuleEntity,
  ): List<JavaResourceRootPropertiesEntity> {
    val contentRootEntities = addContentRootEntities(entitiesToAdd, parentModuleEntity)
    return (entitiesToAdd zip contentRootEntities).map { (entityToAdd, contentRootEntity) ->
      val sourceRoot = addSourceRootEntity(
        workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
        contentRootEntity,
        entityToAdd,
        parentModuleEntity,
      )
      addJavaResourceRootEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd, sourceRoot)
    }
  }

  private suspend fun addContentRootEntities(entitiesToAdd: List<ResourceRoot>, parentModuleEntity: ModuleEntity): List<ContentRootEntity> {
    val contentRoots =
      entitiesToAdd.map { entityToAdd ->
        ContentRoot(
          path = entityToAdd.resourcePath,
          excluded = entityToAdd.excluded,
        )
      }

    return contentRootEntityUpdater.addEntities(contentRoots, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: ResourceRoot,
    parentModuleEntity: ModuleEntity,
  ): SourceRootEntity {
    val entity =
      SourceRootEntity(
        url = entityToAdd.resourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        rootTypeId = entityToAdd.rootType,
        entitySource = parentModuleEntity.entitySource,
      )

    val updatedContentRootEntity =
      builder.modifyContentRootEntity(contentRootEntity) {
        this.sourceRoots += entity
      }

    return updatedContentRootEntity.sourceRoots.last()
  }

  private fun addJavaResourceRootEntity(
    builder: MutableEntityStorage,
    resourceRoot: ResourceRoot,
    sourceRoot: SourceRootEntity,
  ): JavaResourceRootPropertiesEntity {
    val entity =
      JavaResourceRootPropertiesEntity(
        generated = DEFAULT_GENERATED,
        relativeOutputPath = resourceRoot.relativePath ?: DEFAULT_RELATIVE_OUTPUT_PATH,
        entitySource = sourceRoot.entitySource,
      )

    val updatedSourceRoot =
      builder.modifySourceRootEntity(sourceRoot) {
        this.javaResourceRoots += entity
      }

    return updatedSourceRoot.javaResourceRoots.last()
  }

  override suspend fun addEntity(entityToAdd: ResourceRoot, parentModuleEntity: ModuleEntity): JavaResourceRootPropertiesEntity =
    addEntities(listOf(entityToAdd), parentModuleEntity).single()

  private companion object {
    private const val DEFAULT_GENERATED = false
    private const val DEFAULT_RELATIVE_OUTPUT_PATH = ""
  }
}
