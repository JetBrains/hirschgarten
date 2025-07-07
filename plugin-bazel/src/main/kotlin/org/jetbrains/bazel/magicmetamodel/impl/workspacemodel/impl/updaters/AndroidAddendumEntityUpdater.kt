package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.AndroidAddendum
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.AndroidAddendumEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.AndroidTargetType
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.androidAddendumEntity
import org.jetbrains.bsp.protocol.AndroidTargetType.APP
import org.jetbrains.bsp.protocol.AndroidTargetType.LIBRARY
import org.jetbrains.bsp.protocol.AndroidTargetType.TEST
import java.nio.file.Path

internal class AndroidAddendumEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithParentModuleUpdater<AndroidAddendum, AndroidAddendumEntity> {
  override suspend fun addEntity(entityToAdd: AndroidAddendum, parentModuleEntity: ModuleEntity): AndroidAddendumEntity {
    val androidTargetType =
      when (entityToAdd.androidTargetType) {
        APP -> AndroidTargetType.APP
        LIBRARY -> AndroidTargetType.LIBRARY
        TEST -> AndroidTargetType.TEST
      }

    val entity =
      with(entityToAdd) {
        AndroidAddendumEntity(
          entitySource = parentModuleEntity.entitySource,
          androidSdkName = androidSdkName,
          androidTargetType = androidTargetType,
          // Convert to HashMap to avoid serialization errors with a custom map type
          manifestOverrides = HashMap(entityToAdd.manifestOverrides),
          resourceDirectories = entityToAdd.resourceDirectories.map { it.toVirtualFileUrl() },
          assetsDirectories = entityToAdd.assetsDirectories.map { it.toVirtualFileUrl() },
        ) {
          this.manifest = entityToAdd.manifest?.toVirtualFileUrl()
          this.resourceJavaPackage = entityToAdd.resourceJavaPackage
          this.apk = entityToAdd.apk?.toVirtualFileUrl()
        }
      }

    val updatedParentModuleEntity =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.modifyModuleEntity(parentModuleEntity) {
        this.androidAddendumEntity = entity
      }
    return updatedParentModuleEntity.androidAddendumEntity ?: error("androidAddendumEntity was not added properly")
  }

  private fun Path.toVirtualFileUrl(): VirtualFileUrl = this.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
}
