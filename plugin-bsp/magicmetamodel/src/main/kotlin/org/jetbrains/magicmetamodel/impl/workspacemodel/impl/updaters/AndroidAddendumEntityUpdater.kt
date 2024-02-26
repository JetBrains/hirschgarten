package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bsp.protocol.AndroidTargetType.APP
import org.jetbrains.bsp.protocol.AndroidTargetType.LIBRARY
import org.jetbrains.bsp.protocol.AndroidTargetType.TEST
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.workspacemodel.entities.AndroidAddendumEntity
import org.jetbrains.workspacemodel.entities.AndroidTargetType
import java.net.URI
import kotlin.io.path.toPath

internal class AndroidAddendumEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) {
  fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): AndroidAddendumEntity? {
    val androidAddendum = entityToAdd.androidAddendum ?: return null

    val androidTargetType = when (androidAddendum.androidTargetType) {
      APP -> AndroidTargetType.APP
      LIBRARY -> AndroidTargetType.LIBRARY
      TEST -> AndroidTargetType.TEST
    }

    val entity = with(androidAddendum) {
      AndroidAddendumEntity(
        entitySource = parentModuleEntity.entitySource,
        androidSdkName = androidSdkName,
        androidTargetType = androidTargetType,
        resourceFolders = androidAddendum.resourceFolders.map { it.toVirtualFileUrl() },
      ) {
        this.manifest = androidAddendum.manifest?.toVirtualFileUrl()
        this.module = parentModuleEntity
      }
    }
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(entity)
  }

  private fun URI.toVirtualFileUrl(): VirtualFileUrl =
    this.toPath().toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
}
