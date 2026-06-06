package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity
import org.jetbrains.bazel.workspacemodel.entities.jvmBinaryJarsEntity
import org.jetbrains.bsp.protocol.SourceFileCollection

// RC: replaces `JvmBinaryJarsEntityUpdater`
@ApiStatus.Internal
object JvmBinaryJarsBuilder {
  fun write(
      binaryJars: SourceFileCollection,
      parentModuleEntity: ModuleEntity,
      virtualFileUrlManager: VirtualFileUrlManager,
      storage: MutableEntityStorage,
  ) {
    if (binaryJars.isEmpty()) {
      return
    }
    val entity =
      JvmBinaryJarsEntity(
        entitySource = parentModuleEntity.entitySource,
        jars = binaryJars.getFiles()
          .map { it.toVirtualFileUrl(virtualFileUrlManager) }
          .toList(),
      )
    storage.modifyModuleEntity(parentModuleEntity) {
      this.jvmBinaryJarsEntity = entity
    }
  }
}
