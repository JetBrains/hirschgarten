package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import java.nio.file.Path

internal data class SourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val isFile: Boolean,
) : WorkspaceModelEntity()

internal data class JavaSourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val packagePrefix: String,
  val rootType: String,
  val excludedPaths: List<Path> = ArrayList(),
  val targetId: BuildTargetIdentifier
) : WorkspaceModelEntity()

internal class JavaSourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, JavaSourceRootPropertiesEntity> {

  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(
    entityToAdd: JavaSourceRoot,
    parentModuleEntity: ModuleEntity
  ): JavaSourceRootPropertiesEntity {
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
      url = entityToAdd.sourcePath,
      excludedPaths = entityToAdd.excludedPaths,
    )

    return contentRootEntityUpdater.addEntity(contentRoot, parentModuleEntity)
  }

  private fun addSourceRootEntity(
    builder: MutableEntityStorage,
    contentRootEntity: ContentRootEntity,
    entityToAdd: JavaSourceRoot,
  ): SourceRootEntity =
    builder.addEntity(
      SourceRootEntity(
        url = entityToAdd.sourcePath.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        rootType = entityToAdd.rootType,
        entitySource = BspEntitySource
      ) {
        this.contentRoot = contentRootEntity
      }
    )

  private fun addJavaSourceRootEntity(
    builder: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
    entityToAdd: JavaSourceRoot,
  ): JavaSourceRootPropertiesEntity =
    builder.addEntity(
      JavaSourceRootPropertiesEntity(
        generated = entityToAdd.generated,
        packagePrefix = entityToAdd.packagePrefix,
        entitySource = sourceRoot.entitySource
      ) {
        this.sourceRoot = sourceRoot
      }
    )
}
