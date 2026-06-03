package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity
import org.jetbrains.bazel.workspacemodel.entities.scalaAddendumEntity
import org.jetbrains.bsp.protocol.ScalaBuildTarget

// RC: replaces `ScalaAddendumEntityUpdater`; the old `ScalaAddendum` wrapper is dropped,
// we go straight from `ScalaBuildTarget` to `ScalaAddendumEntity`
@ApiStatus.Internal
object ScalaAddendumBuilder {
  fun write(
    scalaBuildTarget: ScalaBuildTarget?,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
  ) {
    val target = scalaBuildTarget ?: return
    val entity =
      ScalaAddendumEntity(
        entitySource = parentModuleEntity.entitySource,
        compilerVersion = target.scalaVersion,
        scalacOptions = target.scalacOptions,
        sdkClasspaths = target.sdkJars.map { it.toVirtualFileUrl(virtualFileUrlManager) },
      )
    storage.modifyModuleEntity(parentModuleEntity) {
      this.scalaAddendumEntity = entity
    }
  }
}
