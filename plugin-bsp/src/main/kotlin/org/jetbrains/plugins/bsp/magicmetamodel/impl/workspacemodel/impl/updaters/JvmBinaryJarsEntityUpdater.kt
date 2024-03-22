package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.workspacemodel.entities.JvmBinaryJarsEntity

internal class JvmBinaryJarsEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaModule, JvmBinaryJarsEntity> {
  override fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): JvmBinaryJarsEntity {
    val jvmBinaryJars = entityToAdd.jvmBinaryJars.map { jar ->
      jar.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
    }
    val entity = JvmBinaryJarsEntity(
      entitySource = parentModuleEntity.entitySource,
      jars = jvmBinaryJars,
    ) { this.module = parentModuleEntity }
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(entity)
  }
}
