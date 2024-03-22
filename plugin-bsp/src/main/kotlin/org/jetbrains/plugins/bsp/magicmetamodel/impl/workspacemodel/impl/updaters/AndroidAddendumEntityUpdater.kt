package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bsp.protocol.AndroidTargetType.APP
import org.jetbrains.bsp.protocol.AndroidTargetType.LIBRARY
import org.jetbrains.bsp.protocol.AndroidTargetType.TEST
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.AndroidAddendum
import org.jetbrains.workspacemodel.entities.AndroidAddendumEntity
import org.jetbrains.workspacemodel.entities.AndroidTargetType
import java.nio.file.Path

internal class AndroidAddendumEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<AndroidAddendum, AndroidAddendumEntity> {
  override fun addEntity(entityToAdd: AndroidAddendum, parentModuleEntity: ModuleEntity): AndroidAddendumEntity {
    val androidTargetType = when (entityToAdd.androidTargetType) {
      APP -> AndroidTargetType.APP
      LIBRARY -> AndroidTargetType.LIBRARY
      TEST -> AndroidTargetType.TEST
    }

    val entity = with(entityToAdd) {
      AndroidAddendumEntity(
        entitySource = parentModuleEntity.entitySource,
        androidSdkName = androidSdkName,
        androidTargetType = androidTargetType,
        resourceFolders = entityToAdd.resourceFolders.map { it.toVirtualFileUrl() },
      ) {
        this.manifest = entityToAdd.manifest?.toVirtualFileUrl()
        this.module = parentModuleEntity
      }
    }
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(entity)
  }

  private fun Path.toVirtualFileUrl(): VirtualFileUrl =
    this.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
}
