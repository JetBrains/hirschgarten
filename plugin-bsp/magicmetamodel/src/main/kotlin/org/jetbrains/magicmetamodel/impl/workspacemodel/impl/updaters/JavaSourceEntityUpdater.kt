package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaSourceRoot

internal class JavaSourceEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, JavaSourceRootPropertiesEntity> {

  private val sourceEntityUpdater = SourceEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(
    entityToAdd: JavaSourceRoot,
    parentModuleEntity: ModuleEntity
  ): JavaSourceRootPropertiesEntity {
    val sourceRootEntity = sourceEntityUpdater.addEntity(entityToAdd.run {
      GenericSourceRoot(
        sourcePath,
        rootType,
        excludedPaths,
      )
    }, parentModuleEntity)

    return addJavaSourceRootEntity(
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
      sourceRootEntity,
      entityToAdd
    )
  }

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
