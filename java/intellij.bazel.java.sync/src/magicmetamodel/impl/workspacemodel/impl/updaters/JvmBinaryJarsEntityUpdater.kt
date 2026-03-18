package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity
import org.jetbrains.bazel.workspacemodel.entities.jvmBinaryJarsEntity

internal class JvmBinaryJarsEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<JavaModule, JvmBinaryJarsEntity> {
  override suspend fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): JvmBinaryJarsEntity {
    val jvmBinaryJars =
      entityToAdd.jvmBinaryJars.map { jar ->
        jar.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
      }
    val entity =
      JvmBinaryJarsEntity(
        entitySource = parentModuleEntity.entitySource,
        jars = jvmBinaryJars,
      )
    val updatedParentModuleEntity =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.modifyModuleEntity(parentModuleEntity) {
        this.jvmBinaryJarsEntity = entity
      }

    return updatedParentModuleEntity.jvmBinaryJarsEntity ?: error("jvmBinaryJarsEntity was not added properly")
  }
}
