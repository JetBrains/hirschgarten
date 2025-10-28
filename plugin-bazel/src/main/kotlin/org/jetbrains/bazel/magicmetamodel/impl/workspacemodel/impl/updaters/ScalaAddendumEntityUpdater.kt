package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendum
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity
import org.jetbrains.bazel.workspacemodel.entities.scalaAddendumEntity

internal class ScalaAddendumEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<ScalaAddendum, ScalaAddendumEntity> {
  override suspend fun addEntity(entityToAdd: ScalaAddendum, parentModuleEntity: ModuleEntity): ScalaAddendumEntity {
    val entity =
      with(entityToAdd) {
        ScalaAddendumEntity(
          entitySource = parentModuleEntity.entitySource,
          compilerVersion = scalaVersion,
          scalacOptions = scalacOptions,
          sdkClasspaths = sdkJars.map { it.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager) },
        )
      }

    val updatedModuleEntity =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.modifyModuleEntity(parentModuleEntity) {
        this.scalaAddendumEntity = entity
      }

    return updatedModuleEntity.scalaAddendumEntity ?: error("ScalaAddendumEntity was not added properly")
  }
}
