package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.WorkspaceEntityStorageBuilder
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addSourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class JavaResourceRoot(
  val resourcePath: Path,
) : WorkspaceModelEntity()

internal class JavaResourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaResourceRoot, JavaResourceRootEntity> {

  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: JavaResourceRoot, parentModuleEntity: ModuleEntity): JavaResourceRootEntity {
    val contentRootEntity = addContentRootEntity(entityToAdd, parentModuleEntity)

    return workspaceModelEntityUpdaterConfig.workspaceModel.updateProjectModel {
      val sourceRoot = addSourceRootEntity(it, contentRootEntity, entityToAdd)
      addJavaResourceRootEntity(it, sourceRoot)
    }
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
    builder: WorkspaceEntityStorageBuilder,
    contentRootEntity: ContentRootEntity,
    entityToAdd: JavaResourceRoot,
  ): SourceRootEntity = builder.addSourceRootEntity(
    contentRoot = contentRootEntity,
    url = entityToAdd.resourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
    rootType = ROOT_TYPE,
    source = workspaceModelEntityUpdaterConfig.projectConfigSource,
  )

  private fun addJavaResourceRootEntity(
    builder: WorkspaceEntityStorageBuilder,
    sourceRoot: SourceRootEntity,
  ): JavaResourceRootEntity = builder.addJavaResourceRootEntity(
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
